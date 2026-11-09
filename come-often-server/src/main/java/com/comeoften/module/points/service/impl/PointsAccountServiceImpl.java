package com.comeoften.module.points.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.PageResult;
import com.comeoften.config.PointsProperties;
import com.comeoften.module.auth.entity.Member;
import com.comeoften.module.auth.mapper.MemberMapper;
import com.comeoften.module.order.constant.OrderStatus;
import com.comeoften.module.order.entity.TradeOrder;
import com.comeoften.module.order.mapper.TradeOrderMapper;
import com.comeoften.module.points.constant.PointsBizType;
import com.comeoften.module.points.constant.PointsChangeType;
import com.comeoften.module.points.converter.PointsConverter;
import com.comeoften.module.points.dto.PointsProfileDTO;
import com.comeoften.module.points.entity.MemberLevel;
import com.comeoften.module.points.entity.PointsAccount;
import com.comeoften.module.points.entity.PointsRecord;
import com.comeoften.module.points.mapper.MemberLevelMapper;
import com.comeoften.module.points.mapper.PointsAccountMapper;
import com.comeoften.module.points.mapper.PointsRecordMapper;
import com.comeoften.module.points.service.PointsAccountService;
import com.comeoften.module.points.util.PointsAmountCalculator;
import com.comeoften.module.points.util.PointsBizKeys;
import com.comeoften.module.points.vo.PointsAccountVO;
import com.comeoften.module.points.vo.PointsRecordVO;
import com.comeoften.module.points.vo.PointsRuleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 积分账户实现。
 *
 * <p>幂等三层：消费端 Redis 消息去重 → points_record.uk_points_idempotent(member_id, biz_type, biz_id)
 * 唯一索引 → 余额写入用带余额条件的 UPDATE。抵现不做裸扣减：下单写 FREEZE，支付成功把同一行
 * 改为 USE（余额在冻结时已扣），取消 / 超时写一笔 ROLLBACK 冲销。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsAccountServiceImpl implements PointsAccountService {

    private static final String DEFAULT_REMARK = "积分入账";

    private final PointsAccountMapper pointsAccountMapper;

    private final PointsRecordMapper pointsRecordMapper;

    private final MemberLevelMapper memberLevelMapper;

    private final MemberMapper memberMapper;

    private final TradeOrderMapper tradeOrderMapper;

    private final PointsProperties pointsProperties;

    @Override
    public PointsAccountVO account(Long memberId) {
        PointsAccount account = pointsAccountMapper.selectByMemberId(memberId);
        PointsAccountVO vo = PointsConverter.toAccountVO(account, levelOf(memberId));
        String today = PointsBizKeys.byDay(LocalDate.now());
        vo.setSignedToday(pointsRecordMapper.countByBiz(memberId, PointsBizType.SIGN_IN, today) > 0);
        vo.setSharedToday(pointsRecordMapper.countByBiz(memberId, PointsBizType.SHARE, today) > 0);
        return vo;
    }

    @Override
    public PageResult<PointsRecordVO> recordPage(Long memberId, Long current, Long size) {
        IPage<PointsRecord> page = pointsRecordMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<PointsRecord>()
                        .eq(PointsRecord::getMemberId, memberId)
                        // 0 值流水是过期批处理的「已处理」标记，对顾客没有信息量，不进明细
                        .ne(PointsRecord::getChangeAmount, 0)
                        .orderByDesc(PointsRecord::getCreateTime));
        List<PointsRecordVO> records = page.getRecords().stream()
                .map(PointsConverter::toRecordVO).collect(Collectors.toList());
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointsAccountVO signIn(Long memberId) {
        boolean granted = grantOnce(memberId, PointsBizType.SIGN_IN, PointsBizKeys.byDay(LocalDate.now()),
                pointsProperties.getSignInPoints(), "每日签到");
        if (!granted) {
            throw new BizException(ErrorCode.POINTS_SIGN_IN_DUPLICATE);
        }
        return accountWithEarned(memberId, pointsProperties.getSignInPoints());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointsAccountVO share(Long memberId) {
        boolean granted = grantOnce(memberId, PointsBizType.SHARE, PointsBizKeys.byDay(LocalDate.now()),
                pointsProperties.getSharePoints(), "每日分享");
        if (!granted) {
            throw new BizException(ErrorCode.POINTS_SHARE_DUPLICATE);
        }
        return accountWithEarned(memberId, pointsProperties.getSharePoints());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointsAccountVO completeProfile(Long memberId, PointsProfileDTO dto) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BizException(ErrorCode.POINTS_ACCOUNT_NOT_FOUND, "会员不存在");
        }
        // 手机号是登录账号唯一标识，只能由微信手机号授权变更，资料接口只放行昵称
        boolean changed = !dto.getNickname().equals(member.getNickname());
        if (changed) {
            member.setNickname(dto.getNickname());
        }
        if (!changed) {
            // 资料没有变化时不做无意义的更新；积分已领过则按「已领取」提示，避免重复提交刷分
            boolean granted = grantOnce(memberId, PointsBizType.PROFILE, PointsBizKeys.profile(),
                    pointsProperties.getProfilePoints(), "完善资料");
            if (!granted) {
                throw new BizException(ErrorCode.POINTS_PROFILE_COMPLETED);
            }
            return accountWithEarned(memberId, pointsProperties.getProfilePoints());
        }
        // 资料有变化：先落库再发分。积分已领过只表示本次不再发放，
        // 绝不能因此回滚用户的资料更新（否则第二次改昵称永远失败）。
        memberMapper.updateById(member);
        boolean granted = grantOnce(memberId, PointsBizType.PROFILE, PointsBizKeys.profile(),
                pointsProperties.getProfilePoints(), "完善资料");
        return accountWithEarned(memberId, granted ? pointsProperties.getProfilePoints() : 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int earn(Long memberId, String bizType, String bizId, int points, String remark) {
        return grantOnce(memberId, bizType, bizId, points, remark) ? points : 0;
    }

    @Override
    public int maxUsablePoints(Long memberId, BigDecimal afterDiscountAmount) {
        return PointsAmountCalculator.maxUsablePoints(balanceOf(memberId), afterDiscountAmount,
                pointsProperties.getExchangeRate());
    }

    @Override
    public int resolveUsePoints(Long memberId, Integer requested, BigDecimal afterDiscountAmount) {
        if (requested == null || requested <= 0) {
            return 0;
        }
        return PointsAmountCalculator.resolveUsedPoints(requested, balanceOf(memberId),
                afterDiscountAmount, pointsProperties.getExchangeRate());
    }

    @Override
    public BigDecimal amountOf(int points) {
        return PointsAmountCalculator.amount(points, pointsProperties.getExchangeRate());
    }

    @Override
    public int balanceOf(Long memberId) {
        PointsAccount account = pointsAccountMapper.selectByMemberId(memberId);
        return account == null || account.getBalance() == null ? 0 : account.getBalance();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int freezeForOrder(Long memberId, String orderNo, Integer points) {
        if (memberId == null || !StringUtils.hasText(orderNo) || points == null || points <= 0) {
            return 0;
        }
        String bizId = PointsBizKeys.orderRedeem(orderNo);
        if (pointsRecordMapper.countByBiz(memberId, PointsBizType.ORDER, bizId) > 0) {
            log.info("积分冻结已存在，跳过重复冻结: orderNo={}", orderNo);
            return 0;
        }
        ensureAccount(memberId);
        // 带余额条件的 UPDATE 是并发扣减的唯一判据，影响行数为 0 即余额不足
        if (pointsAccountMapper.deduct(memberId, points) == 0) {
            throw new BizException(ErrorCode.POINTS_BALANCE_NOT_ENOUGH,
                    "可用积分不足 " + points + " 分，请刷新后重试");
        }
        PointsAccount account = pointsAccountMapper.selectByMemberId(memberId);
        PointsRecord record = new PointsRecord();
        record.setMemberId(memberId);
        record.setChangeType(PointsChangeType.FREEZE.getCode());
        record.setChangeAmount(-points);
        record.setBizType(PointsBizType.ORDER);
        record.setBizId(bizId);
        record.setBalanceAfter(balanceOfAccount(account));
        record.setRemark("积分抵现冻结，订单号 " + orderNo);
        pointsRecordMapper.insert(record);
        log.info("积分已冻结: orderNo={}, points={}, balanceAfter={}", orderNo, points,
                record.getBalanceAfter());
        return points;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmUseForOrder(Long memberId, String orderNo) {
        if (memberId == null || !StringUtils.hasText(orderNo)) {
            return;
        }
        String bizId = PointsBizKeys.orderRedeem(orderNo);
        if (pointsRecordMapper.countByBiz(memberId, PointsBizType.ORDER,
                PointsBizKeys.orderRollback(orderNo)) > 0) {
            // 已写过回滚流水：冻结与实扣互斥，这里不能再把冻结改成实扣
            log.info("订单积分已回滚，跳过冻结转实扣: orderNo={}", orderNo);
            return;
        }
        PointsRecord record = pointsRecordMapper.selectByBiz(memberId, PointsBizType.ORDER, bizId);
        if (record == null || !PointsChangeType.FREEZE.getCode().equals(record.getChangeType())) {
            return;
        }
        // 带旧状态条件的 UPDATE：并发或重复消费时只有一次能把 FREEZE 改成 USE
        if (pointsRecordMapper.changeStatus(memberId, PointsBizType.ORDER, bizId,
                PointsChangeType.FREEZE.getCode(), PointsChangeType.USE.getCode()) == 0) {
            return;
        }
        pointsAccountMapper.addUsed(memberId, Math.abs(record.getChangeAmount()));
        log.info("积分冻结转实扣: orderNo={}, points={}", orderNo, Math.abs(record.getChangeAmount()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackForOrder(Long memberId, String orderNo) {
        if (memberId == null || !StringUtils.hasText(orderNo)) {
            return;
        }
        PointsRecord freeze = pointsRecordMapper.selectByBiz(memberId, PointsBizType.ORDER,
                PointsBizKeys.orderRedeem(orderNo));
        if (freeze == null || !PointsChangeType.FREEZE.getCode().equals(freeze.getChangeType())) {
            // 未冻结或已转实扣（已支付订单不可取消），无需回滚
            return;
        }
        String rollbackBizId = PointsBizKeys.orderRollback(orderNo);
        if (pointsRecordMapper.countByBiz(memberId, PointsBizType.ORDER, rollbackBizId) > 0) {
            return;
        }
        int points = Math.abs(freeze.getChangeAmount());
        PointsRecord rollback = new PointsRecord();
        rollback.setMemberId(memberId);
        rollback.setChangeType(PointsChangeType.ROLLBACK.getCode());
        rollback.setChangeAmount(points);
        rollback.setBizType(PointsBizType.ORDER);
        rollback.setBizId(rollbackBizId);
        rollback.setBalanceAfter(balanceOf(memberId) + points);
        rollback.setRemark("订单取消退回积分，订单号 " + orderNo);
        // 先落反向流水再动余额：唯一索引让「回滚」成为一条可见的互斥标记，
        // 支付成功消息晚到时会看到它并放弃把冻结改成实扣
        pointsRecordMapper.insert(rollback);
        pointsAccountMapper.restore(memberId, points);
        log.info("积分冻结已回滚: orderNo={}, points={}", orderNo, points);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int earnForCompletedOrder(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return 0;
        }
        TradeOrder order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getOrderNo, orderNo));
        if (order == null || order.getMemberId() == null) {
            log.warn("订单完成积分入账跳过，订单不存在: orderNo={}", orderNo);
            return 0;
        }
        if (!OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            log.info("订单未完成，跳过积分入账: orderNo={}, status={}", orderNo, order.getStatus());
            return 0;
        }
        int points = PointsAmountCalculator.earnPoints(order.getPayAmount(),
                pointsProperties.getOrderEarnRate());
        if (points <= 0) {
            return 0;
        }
        boolean granted = grantOnce(order.getMemberId(), PointsBizType.ORDER,
                PointsBizKeys.orderEarn(orderNo), points, "订单完成发放，订单号 " + orderNo);
        if (!granted) {
            log.info("订单积分已发放过，忽略重复消息: orderNo={}", orderNo);
            return 0;
        }
        log.info("订单完成积分已发放: orderNo={}, memberId={}, points={}",
                orderNo, order.getMemberId(), points);
        return points;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int expireDue() {
        LocalDateTime now = LocalDateTime.now();
        List<PointsRecord> due = pointsRecordMapper.selectDueForExpire(now,
                pointsProperties.getExpireBatchSize());
        int expiredTotal = 0;
        for (PointsRecord source : due) {
            String bizId = PointsBizKeys.expire(source.getId());
            if (pointsRecordMapper.countByBiz(source.getMemberId(), source.getBizType(), bizId) > 0) {
                continue;
            }
            int deducted = deductForExpire(source);
            // 无论扣没扣到，都落一条流水：它就是「本笔已处理」的标记，
            // 扫描 SQL 用 NOT EXISTS 依赖它把源行排除掉，否则批处理会永远重扫同一批
            PointsRecord expire = new PointsRecord();
            expire.setMemberId(source.getMemberId());
            expire.setChangeType(PointsChangeType.EXPIRE.getCode());
            expire.setChangeAmount(-deducted);
            expire.setBizType(source.getBizType());
            expire.setBizId(bizId);
            expire.setBalanceAfter(balanceOf(source.getMemberId()));
            expire.setRemark(deducted > 0 ? "积分到期扣减" : "积分到期时余额已用尽");
            pointsRecordMapper.insert(expire);
            expiredTotal += deducted;
        }
        if (!due.isEmpty()) {
            log.info("积分过期批处理完成: 扫描={}, 过期积分={}", due.size(), expiredTotal);
        }
        return expiredTotal;
    }

    @Override
    public PointsRuleVO rule() {
        PointsRuleVO vo = new PointsRuleVO();
        vo.setOrderEarnRate(pointsProperties.getOrderEarnRate());
        vo.setExchangeRate(pointsProperties.getExchangeRate());
        vo.setSignInPoints(pointsProperties.getSignInPoints());
        vo.setReviewPoints(pointsProperties.getReviewPoints());
        vo.setSharePoints(pointsProperties.getSharePoints());
        vo.setProfilePoints(pointsProperties.getProfilePoints());
        vo.setValidDays(pointsProperties.getValidDays());
        vo.setExchangeText("每 " + pointsProperties.getExchangeRate() + " 积分抵 1 元，"
                + "抵现后订单至少保留 0.01 元");
        return vo;
    }

    // ------------------------------------------------------------------ 私有逻辑

    /**
     * 过期扣减：以「带余额条件的 UPDATE 是否命中」为准，命中不到时按最新余额重算一次，
     * 绝不写一条实际没扣到钱的流水。
     */
    private int deductForExpire(PointsRecord source) {
        int wanted = Math.max(0, source.getChangeAmount());
        if (wanted <= 0) {
            return 0;
        }
        int amount = Math.min(balanceOf(source.getMemberId()), wanted);
        if (amount > 0 && pointsAccountMapper.deduct(source.getMemberId(), amount) > 0) {
            return amount;
        }
        // 余额可能刚被下单冻结吃掉，按最新余额再试一次
        int retry = Math.min(balanceOf(source.getMemberId()), wanted);
        if (retry > 0 && pointsAccountMapper.deduct(source.getMemberId(), retry) > 0) {
            return retry;
        }
        return 0;
    }

    private PointsAccountVO accountWithEarned(Long memberId, int earned) {
        PointsAccountVO vo = account(memberId);
        vo.setEarnedPoints(earned);
        return vo;
    }

    /**
     * 一次性发放：先按业务幂等键查重，再累加余额与累计获得，最后落流水。
     * 撞唯一索引时异常冒泡，整个事务回滚，由消息重试后的查重分支收敛。
     */
    private boolean grantOnce(Long memberId, String bizType, String bizId, int points, String remark) {
        if (points <= 0) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "积分发放额度未配置，请检查 comeoften.points");
        }
        if (pointsRecordMapper.countByBiz(memberId, bizType, bizId) > 0) {
            return false;
        }
        ensureAccount(memberId);
        if (pointsAccountMapper.addEarned(memberId, points) == 0) {
            throw new BizException(ErrorCode.POINTS_ACCOUNT_NOT_FOUND);
        }
        PointsRecord record = new PointsRecord();
        record.setMemberId(memberId);
        record.setChangeType(PointsChangeType.EARN.getCode());
        record.setChangeAmount(points);
        record.setBizType(bizType);
        record.setBizId(bizId);
        record.setBalanceAfter(balanceOf(memberId));
        record.setExpireAt(LocalDateTime.now().plusDays(pointsProperties.getValidDays()));
        record.setRemark(StringUtils.hasText(remark) ? remark : DEFAULT_REMARK);
        pointsRecordMapper.insert(record);
        return true;
    }

    /**
     * 账户不存在时补建，唯一索引兜住并发插入。
     */
    private PointsAccount ensureAccount(Long memberId) {
        PointsAccount account = pointsAccountMapper.selectByMemberId(memberId);
        if (account != null) {
            return account;
        }
        PointsAccount created = new PointsAccount();
        created.setMemberId(memberId);
        created.setBalance(0);
        created.setTotalEarned(0);
        created.setTotalUsed(0);
        try {
            pointsAccountMapper.insert(created);
            return created;
        } catch (DuplicateKeyException e) {
            return pointsAccountMapper.selectByMemberId(memberId);
        }
    }

    private int balanceOfAccount(PointsAccount account) {
        return account == null || account.getBalance() == null ? 0 : account.getBalance();
    }

    /**
     * 会员当前等级；未设置等级时回落到最低档，避免新用户等级页空白。
     */
    private MemberLevel levelOf(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member != null && member.getLevelId() != null) {
            MemberLevel level = memberLevelMapper.selectById(member.getLevelId());
            if (level != null) {
                return level;
            }
        }
        List<MemberLevel> levels = memberLevelMapper.selectList(new LambdaQueryWrapper<MemberLevel>()
                .orderByAsc(MemberLevel::getSort));
        return levels.isEmpty() ? null : levels.get(0);
    }
}
