package com.comeoften.module.balance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.PageResult;
import com.comeoften.config.BalanceProperties;
import com.comeoften.module.balance.constant.BalanceBizType;
import com.comeoften.module.balance.constant.BalanceChangeType;
import com.comeoften.module.balance.converter.BalanceConverter;
import com.comeoften.module.balance.dto.BalanceRechargeDTO;
import com.comeoften.module.balance.entity.BalanceRecord;
import com.comeoften.module.balance.entity.MemberBalance;
import com.comeoften.module.balance.mapper.BalanceRecordMapper;
import com.comeoften.module.balance.mapper.MemberBalanceMapper;
import com.comeoften.module.balance.service.BalanceService;
import com.comeoften.module.balance.util.BalanceBizKeys;
import com.comeoften.module.balance.util.BalanceGiftCalculator;
import com.comeoften.module.balance.vo.BalanceAccountVO;
import com.comeoften.module.balance.vo.BalanceRechargeVO;
import com.comeoften.module.balance.vo.BalanceRecordVO;
import com.comeoften.integration.pay.PayChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 会员储值实现。
 *
 * <p>三个不变量：① 余额只能通过带条件的 UPDATE 变动；② 每次变动在同一事务写一条流水；
 * ③ 同一 {@code biz_id} 只入账一次（唯一索引兜底）。扣减发生在下单事务内而不是支付成功时：
 * 余额是支付手段，不是优惠，下单未付时已经把「这笔钱」从可用余额里划走了；取消 / 超时写
 * {@code #ROLLBACK}、整单退款写 {@code #REFUND} 冲销。</p>
 *
 * <p>充值当前是 Mock 入账（{@code payChannel=MOCK}）。真实微信支付要等商户号到位后把
 * {@link #recharge} 拆成「预下单 + 回调入账」，见 {@code docs/mock-replacement.md}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceServiceImpl implements BalanceService {

    private static final int AMOUNT_SCALE = 2;

    private static final int MAX_FRACTION_SCALE = 2;

    private static final long PAGE_MAX_SIZE = 50L;

    private static final DateTimeFormatter RECHARGE_NO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MemberBalanceMapper memberBalanceMapper;

    private final BalanceRecordMapper balanceRecordMapper;

    private final BalanceProperties balanceProperties;

    @Override
    public BalanceAccountVO account(Long memberId) {
        return BalanceConverter.toAccountVO(memberBalanceMapper.selectByMemberId(memberId),
                balanceProperties);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BalanceRechargeVO recharge(Long memberId, BalanceRechargeDTO dto) {
        BigDecimal amount = validateAmount(dto == null ? null : dto.getAmount());
        ensureAccount(memberId);
        BigDecimal gift = BalanceGiftCalculator.giftOf(amount, balanceProperties.giftTiers());
        String rechargeNo = generateRechargeNo();
        try {
            if (memberBalanceMapper.addPrincipal(memberId, amount) == 0) {
                throw new BizException(ErrorCode.BALANCE_ACCOUNT_NOT_FOUND);
            }
            BigDecimal afterPrincipal = balanceOf(memberId);
            insertRecord(memberId, BalanceChangeType.RECHARGE, amount, afterPrincipal,
                    BalanceBizType.RECHARGE, BalanceBizKeys.recharge(rechargeNo),
                    "储值充值，单号 " + rechargeNo);
            BigDecimal balanceAfter = afterPrincipal;
            if (gift.signum() > 0) {
                if (memberBalanceMapper.addGift(memberId, gift) == 0) {
                    throw new BizException(ErrorCode.BALANCE_ACCOUNT_NOT_FOUND);
                }
                balanceAfter = balanceOf(memberId);
                insertRecord(memberId, BalanceChangeType.GIFT, gift, balanceAfter,
                        BalanceBizType.RECHARGE, BalanceBizKeys.rechargeGift(rechargeNo),
                        "充值赠送，单号 " + rechargeNo);
            }
            BalanceRechargeVO vo = new BalanceRechargeVO();
            vo.setRechargeNo(rechargeNo);
            vo.setAmount(amount);
            vo.setGift(gift);
            vo.setBalance(balanceAfter);
            vo.setPayChannel(PayChannel.MOCK);
            log.info("储值充值成功: memberId={}, rechargeNo={}, amount={}, gift={}, balance={}",
                    memberId, rechargeNo, amount, gift, balanceAfter);
            return vo;
        } catch (DuplicateKeyException e) {
            // 充值单号是雪花外的新随机串，撞唯一索引只可能是并发重放同一单
            throw new BizException(ErrorCode.BALANCE_RECORD_DUPLICATE, "充值单号重复，请重试", e);
        }
    }

    @Override
    public PageResult<BalanceRecordVO> recordPage(Long memberId, Long current, Long size) {
        Page<BalanceRecord> page = new Page<>(Math.max(1L, current == null ? 1L : current),
                boundSize(size));
        IPage<BalanceRecord> result = balanceRecordMapper.selectPage(page,
                new LambdaQueryWrapper<BalanceRecord>()
                        .eq(BalanceRecord::getMemberId, memberId)
                        .orderByDesc(BalanceRecord::getCreateTime));
        List<BalanceRecordVO> records = result.getRecords().stream()
                .map(BalanceConverter::toRecordVO).collect(Collectors.toList());
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public BigDecimal availableOf(Long memberId) {
        MemberBalance account = memberBalanceMapper.selectByMemberId(memberId);
        return account == null ? zero() : BalanceConverter.amount(account.getBalance());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal deductForOrder(Long memberId, String orderNo, BigDecimal amount) {
        if (memberId == null || orderNo == null || amount == null || amount.signum() <= 0) {
            return zero();
        }
        BigDecimal value = BalanceConverter.amount(amount);
        String bizId = BalanceBizKeys.orderDeduct(orderNo);
        if (balanceRecordMapper.countByBiz(memberId, BalanceBizType.ORDER, bizId) > 0) {
            log.info("订单余额抵扣已存在，跳过重复扣减: orderNo={}", orderNo);
            return zero();
        }
        ensureAccount(memberId);
        if (memberBalanceMapper.deduct(memberId, value) == 0) {
            throw new BizException(ErrorCode.BALANCE_NOT_ENOUGH,
                    "储值余额不足 " + value.toPlainString() + " 元，请调整抵扣金额后重试");
        }
        BigDecimal balanceAfter = balanceOf(memberId);
        insertRecord(memberId, BalanceChangeType.CONSUME, value.negate(), balanceAfter,
                BalanceBizType.ORDER, bizId, "订单抵扣储值余额，订单号 " + orderNo);
        log.info("订单已抵扣储值余额: orderNo={}, amount={}, balanceAfter={}",
                orderNo, value, balanceAfter);
        return value;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackForOrder(Long memberId, String orderNo) {
        if (memberId == null || orderNo == null) {
            return;
        }
        BalanceRecord deduct = balanceRecordMapper.selectByBiz(memberId, BalanceBizType.ORDER,
                BalanceBizKeys.orderDeduct(orderNo));
        if (deduct == null) {
            return;
        }
        String rollbackBizId = BalanceBizKeys.orderRollback(orderNo);
        if (balanceRecordMapper.countByBiz(memberId, BalanceBizType.ORDER, rollbackBizId) > 0) {
            return;
        }
        BigDecimal amount = BalanceConverter.amount(deduct.getChangeAmount()).abs();
        if (amount.signum() <= 0) {
            return;
        }
        if (memberBalanceMapper.restore(memberId, amount) == 0) {
            throw new BizException(ErrorCode.BALANCE_ACCOUNT_NOT_FOUND);
        }
        insertRecord(memberId, BalanceChangeType.ROLLBACK, amount, balanceOf(memberId),
                BalanceBizType.ORDER, rollbackBizId, "订单取消退回储值余额，订单号 " + orderNo);
        log.info("订单余额抵扣已回滚: orderNo={}, amount={}", orderNo, amount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundForOrder(Long memberId, String orderNo, BigDecimal orderBalanceAmount) {
        if (memberId == null || orderNo == null || orderBalanceAmount == null
                || orderBalanceAmount.signum() <= 0) {
            return;
        }
        String bizId = BalanceBizKeys.orderRefund(orderNo);
        if (balanceRecordMapper.countByBiz(memberId, BalanceBizType.REFUND, bizId) > 0) {
            log.info("订单余额退款已存在，跳过重复退回: orderNo={}", orderNo);
            return;
        }
        BigDecimal amount = BalanceConverter.amount(orderBalanceAmount);
        ensureAccount(memberId);
        if (memberBalanceMapper.restore(memberId, amount) == 0) {
            throw new BizException(ErrorCode.BALANCE_ACCOUNT_NOT_FOUND);
        }
        insertRecord(memberId, BalanceChangeType.REFUND, amount, balanceOf(memberId),
                BalanceBizType.REFUND, bizId, "订单退款退回储值余额，订单号 " + orderNo);
        log.info("订单退款已退回储值余额: orderNo={}, amount={}", orderNo, amount);
    }

    // ------------------------------------------------------------------ 私有逻辑

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BizException(ErrorCode.BALANCE_RECHARGE_AMOUNT_INVALID);
        }
        BigDecimal value = amount.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        if (amount.stripTrailingZeros().scale() > MAX_FRACTION_SCALE) {
            throw new BizException(ErrorCode.BALANCE_RECHARGE_AMOUNT_INVALID, "充值金额最多两位小数");
        }
        if (value.compareTo(balanceProperties.getMinRecharge()) < 0
                || value.compareTo(balanceProperties.getMaxRecharge()) > 0) {
            throw new BizException(ErrorCode.BALANCE_RECHARGE_AMOUNT_INVALID,
                    "单笔充值金额需在 " + balanceProperties.getMinRecharge().toPlainString() + " ~ "
                            + balanceProperties.getMaxRecharge().toPlainString() + " 元之间");
        }
        return value;
    }

    /**
     * 账户不存在时补建，唯一索引 {@code uk_member_id} 兜住并发插入。
     */
    private void ensureAccount(Long memberId) {
        if (memberBalanceMapper.selectByMemberId(memberId) != null) {
            return;
        }
        MemberBalance created = new MemberBalance();
        created.setMemberId(memberId);
        created.setBalance(zero());
        created.setTotalRecharge(zero());
        created.setTotalGift(zero());
        created.setTotalConsume(zero());
        try {
            memberBalanceMapper.insert(created);
        } catch (DuplicateKeyException e) {
            log.info("储值账户并发创建，按已存在处理: memberId={}", memberId);
        }
    }

    private BigDecimal balanceOf(Long memberId) {
        MemberBalance account = memberBalanceMapper.selectByMemberId(memberId);
        return account == null ? zero() : BalanceConverter.amount(account.getBalance());
    }

    private void insertRecord(Long memberId, String changeType, BigDecimal changeAmount,
                              BigDecimal balanceAfter, String bizType, String bizId, String remark) {
        BalanceRecord record = new BalanceRecord();
        record.setMemberId(memberId);
        record.setChangeType(changeType);
        record.setChangeAmount(BalanceConverter.amount(changeAmount));
        record.setBalanceAfter(BalanceConverter.amount(balanceAfter));
        record.setBizType(bizType);
        record.setBizId(bizId);
        record.setRemark(remark);
        balanceRecordMapper.insert(record);
    }

    private static String generateRechargeNo() {
        return "RC" + LocalDateTime.now().format(RECHARGE_NO_FORMATTER)
                + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(AMOUNT_SCALE);
    }

    private static long boundSize(Long size) {
        long value = size == null ? 10L : size;
        return Math.min(Math.max(1L, value), PAGE_MAX_SIZE);
    }
}
