package com.comeoften.module.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.PageResult;
import com.comeoften.integration.pay.PayGateway;
import com.comeoften.integration.pay.model.RefundOrder;
import com.comeoften.integration.pay.model.RefundResult;
import com.comeoften.module.order.constant.OperatorType;
import com.comeoften.module.order.constant.OrderStatus;
import com.comeoften.module.order.constant.OrderStatusTransition;
import com.comeoften.module.order.entity.PaymentRecord;
import com.comeoften.module.order.entity.TradeOrder;
import com.comeoften.module.order.mapper.PaymentRecordMapper;
import com.comeoften.module.order.mapper.TradeOrderMapper;
import com.comeoften.module.order.service.OrderService;
import com.comeoften.module.payment.converter.RefundConverter;
import com.comeoften.module.payment.dto.RefundApplyDTO;
import com.comeoften.module.payment.dto.RefundAuditDTO;
import com.comeoften.module.payment.entity.RefundRecord;
import com.comeoften.module.payment.mapper.RefundRecordMapper;
import com.comeoften.module.payment.service.RefundService;
import com.comeoften.module.payment.util.RefundAmountResolver;
import com.comeoften.module.payment.vo.RefundVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 退款编排。计划 3.3 与 4.7：制作前（PAID）可直接退；制作中 / 待取餐（ACCEPTED / READY）
 * 进 REFUNDING 等后台审核；审核驳回回到 ACCEPTED。定金原则上不退，仅商家无法履约时后台主动发起。
 *
 * <p>所有订单状态写都委托 OrderService.transition（内部按 OrderStatusTransition 校验并带旧状态条件
 * 做 CAS UPDATE），这里不自己改 trade_order.status。退款金额只取已收流水，不取整单金额。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private static final int STATUS_PENDING = 0;

    private static final int STATUS_REFUNDED = 1;

    private static final int STATUS_REJECTED = 2;

    private static final int PAY_STATUS_SUCCESS = 1;

    private static final int MAX_REMARK_LENGTH = 250;

    private static final DateTimeFormatter REFUND_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RefundRecordMapper refundRecordMapper;

    private final PaymentRecordMapper paymentRecordMapper;

    private final TradeOrderMapper tradeOrderMapper;

    private final OrderService orderService;

    private final PayGateway payGateway;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundVO applyByMember(Long memberId, Long storeId, String orderNo, RefundApplyDTO dto) {
        TradeOrder order = requireOwned(memberId, storeId, orderNo);
        return apply(order, dto, false, OperatorType.MEMBER, memberId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundVO applyByAdmin(Long storeId, String orderNo, RefundApplyDTO dto, Long operatorId) {
        TradeOrder order = requireByStore(storeId, orderNo);
        return apply(order, dto, true, OperatorType.ADMIN, operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundVO audit(Long storeId, String refundNo, RefundAuditDTO dto, Long operatorId) {
        RefundRecord record = requireByRefundNo(refundNo);
        if (!Integer.valueOf(STATUS_PENDING).equals(record.getStatus())) {
            throw new BizException(ErrorCode.REFUND_STATUS_INVALID, "仅待审核的退款单可审核");
        }
        TradeOrder order = requireByStore(storeId, requireOrder(record).getOrderNo());
        if (!OrderStatus.REFUNDING.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.REFUND_STATUS_INVALID, "订单当前不在退款审核态");
        }
        boolean approved = dto != null && Boolean.TRUE.equals(dto.getApproved());
        int targetStatus = approved ? STATUS_REFUNDED : STATUS_REJECTED;
        LocalDateTime refundTime = approved ? LocalDateTime.now() : null;
        // 并发审核的唯一判据：带旧状态条件的 UPDATE。输家在调用渠道退款之前就被挡住，
        // 不会出现「一个审核人已退款、另一个把单据驳回」或同一退款单被渠道受理两次。
        LambdaUpdateWrapper<RefundRecord> update = new LambdaUpdateWrapper<RefundRecord>()
                .eq(RefundRecord::getId, record.getId())
                .eq(RefundRecord::getStatus, STATUS_PENDING)
                .set(RefundRecord::getStatus, targetStatus)
                .set(RefundRecord::getAuditUserId, operatorId);
        if (refundTime != null) {
            update.set(RefundRecord::getRefundTime, refundTime);
        }
        if (refundRecordMapper.update(null, update) == 0) {
            throw new BizException(ErrorCode.REFUND_STATUS_INVALID, "退款单已被其他审核人处理");
        }
        OrderStatus target = approved ? OrderStatus.REFUNDED : OrderStatus.ACCEPTED;
        String remark = dto != null && StringUtils.hasText(dto.getRemark())
                ? dto.getRemark() : (approved ? "退款审核通过" : "退款审核驳回");
        // 订单状态 CAS 成功之后才调渠道；渠道失败抛异常会连同状态与退款单一起回滚。
        orderService.transition(storeId, order.getOrderNo(), target.name(), trim(remark),
                OperatorType.ADMIN, operatorId);
        if (approved) {
            callGateway(order, requireSuccessPayment(order), record);
        }
        record.setStatus(targetStatus);
        record.setAuditUserId(operatorId);
        record.setRefundTime(refundTime);
        log.info("退款审核完成: refundNo={}, orderNo={}, approved={}, operatorId={}",
                refundNo, order.getOrderNo(), approved, operatorId);
        return toVO(record);
    }

    @Override
    public RefundVO detailForMember(Long memberId, Long storeId, String orderNo) {
        TradeOrder order = requireOwned(memberId, storeId, orderNo);
        RefundRecord record = findLatestRefund(order.getId());
        if (record == null) {
            throw new BizException(ErrorCode.REFUND_RECORD_NOT_FOUND);
        }
        return RefundConverter.toVO(record, order);
    }

    @Override
    public PageResult<RefundVO> pageForAdmin(Long storeId, Long current, Long size, Integer status) {
        IPage<RefundRecord> page = refundRecordMapper.pageByStore(new Page<>(current, size), storeId, status);
        List<Long> orderIds = page.getRecords().stream().map(RefundRecord::getOrderId)
                .distinct().collect(Collectors.toList());
        Map<Long, TradeOrder> orders = orderIds.isEmpty() ? Collections.emptyMap()
                : tradeOrderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(TradeOrder::getId, Function.identity(), (left, right) -> left));
        Map<Long, TradeOrder> finalOrders = orders;
        List<RefundVO> records = page.getRecords().stream()
                .map(record -> RefundConverter.toVO(record, finalOrders.get(record.getOrderId())))
                .collect(Collectors.toList());
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    private RefundVO apply(TradeOrder order, RefundApplyDTO dto, boolean merchantInitiated,
                           OperatorType operatorType, Long operatorId) {
        OrderStatus from = OrderStatus.of(order.getStatus());
        // 迁移表里 PAID 只能进 REFUNDING；「制作前可退、无需人工等待」用同一事务内立即审核通过实现，
        // 不新增 PAID→REFUNDED 的旁路规则，状态机仍是唯一真相源。
        if (!OrderStatusTransition.canTransition(from, OrderStatus.REFUNDING)) {
            throw new BizException(ErrorCode.REFUND_NOT_ALLOWED,
                    "当前状态不支持退款：" + (from == null ? order.getStatus() : from.getDescription()));
        }
        if (hasDeposit(order) && !merchantInitiated) {
            throw new BizException(ErrorCode.REFUND_DEPOSIT_MERCHANT_ONLY);
        }
        PaymentRecord payment = requireSuccessPayment(order);
        if (findActiveRefund(order.getId()) != null) {
            throw new BizException(ErrorCode.REFUND_ALREADY_APPLIED);
        }
        BigDecimal refundAmount = RefundAmountResolver.refundable(
                payment.getPayAmount(), sumRefunded(order.getId()));
        if (refundAmount.signum() <= 0) {
            throw new BizException(ErrorCode.REFUND_AMOUNT_INVALID, "该订单没有可退金额");
        }
        String reason = dto == null || !StringUtils.hasText(dto.getReason())
                ? (merchantInitiated ? "商家无法履约，主动退还" : "顾客申请退款")
                : trim(dto.getReason());
        RefundRecord record = new RefundRecord();
        record.setRefundNo(generateRefundNo());
        record.setOrderId(order.getId());
        record.setPaymentId(payment.getId());
        record.setRefundAmount(refundAmount);
        record.setReason(reason);
        record.setStatus(STATUS_PENDING);
        boolean direct = from == OrderStatus.PAID;
        String remark = (merchantInitiated ? "商家主动发起退款" : "顾客申请退款")
                + "，金额 " + refundAmount.toPlainString() + " 元；原因：" + reason;
        // 先用订单状态的 CAS 抢占退款资格：并发重复申请只有一个能成功，不依赖退款单查重读后判。
        orderService.transition(order.getStoreId(), order.getOrderNo(), OrderStatus.REFUNDING.name(),
                trim(remark), operatorType, operatorId);
        try {
            refundRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.REFUND_ALREADY_APPLIED, "退款申请并发冲突，请重试", e);
        }
        if (direct) {
            // 两次状态迁移都完成、订单已确定为 REFUNDED，再调渠道退款；任一环节失败事务整体回滚。
            orderService.transition(order.getStoreId(), order.getOrderNo(), OrderStatus.REFUNDED.name(),
                    "制作前退款自动审核通过，金额 " + refundAmount.toPlainString() + " 元",
                    operatorType, operatorId);
            callGateway(order, payment, record);
            record.setStatus(STATUS_REFUNDED);
            record.setAuditUserId(operatorId);
            record.setRefundTime(LocalDateTime.now());
            refundRecordMapper.updateById(record);
        }
        log.info("退款申请已提交: refundNo={}, orderNo={}, from={}, direct={}, amount={}",
                record.getRefundNo(), order.getOrderNo(), from, direct, refundAmount);
        return toVO(record);
    }

    private void callGateway(TradeOrder order, PaymentRecord payment, RefundRecord record) {
        RefundOrder refundOrder = new RefundOrder();
        refundOrder.setRefundNo(record.getRefundNo());
        refundOrder.setOutTradeNo(order.getOrderNo());
        refundOrder.setTradeNo(payment.getTradeNo());
        refundOrder.setRefundAmount(record.getRefundAmount());
        refundOrder.setTotalAmount(payment.getPayAmount());
        refundOrder.setReason(record.getReason());
        RefundResult result = payGateway.refund(refundOrder);
        if (result == null || !Boolean.TRUE.equals(result.getSuccess())) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "支付渠道未受理退款");
        }
        log.info("渠道退款受理成功: refundNo={}, channelRefundNo={}",
                record.getRefundNo(), result.getChannelRefundNo());
    }

    private PaymentRecord requireSuccessPayment(TradeOrder order) {
        PaymentRecord payment = paymentRecordMapper.selectOne(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOutTradeNo, order.getOrderNo()));
        if (payment == null || !Integer.valueOf(PAY_STATUS_SUCCESS).equals(payment.getStatus())) {
            throw new BizException(ErrorCode.PAYMENT_RECORD_NOT_FOUND, "订单没有已成功的支付流水");
        }
        return payment;
    }

    private RefundRecord findActiveRefund(Long orderId) {
        return refundRecordMapper.selectOne(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getOrderId, orderId)
                .in(RefundRecord::getStatus, Arrays.asList(STATUS_PENDING, STATUS_REFUNDED))
                .last("LIMIT 1"));
    }

    private RefundRecord findLatestRefund(Long orderId) {
        return refundRecordMapper.selectOne(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getOrderId, orderId)
                .orderByDesc(RefundRecord::getCreateTime)
                .last("LIMIT 1"));
    }

    private BigDecimal sumRefunded(Long orderId) {
        return refundRecordMapper.selectList(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getOrderId, orderId)
                .eq(RefundRecord::getStatus, STATUS_REFUNDED))
                .stream().map(RefundRecord::getRefundAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private RefundRecord requireByRefundNo(String refundNo) {
        RefundRecord record = refundRecordMapper.selectOne(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getRefundNo, refundNo));
        if (record == null) {
            throw new BizException(ErrorCode.REFUND_RECORD_NOT_FOUND);
        }
        return record;
    }

    private TradeOrder requireOrder(RefundRecord record) {
        TradeOrder order = tradeOrderMapper.selectById(record.getOrderId());
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private TradeOrder requireByStore(Long storeId, String orderNo) {
        TradeOrder order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getOrderNo, orderNo));
        if (order == null || !storeId.equals(order.getStoreId())) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private TradeOrder requireOwned(Long memberId, Long storeId, String orderNo) {
        TradeOrder order = requireByStore(storeId, orderNo);
        if (!memberId.equals(order.getMemberId())) {
            throw new BizException(ErrorCode.ORDER_ACCESS_DENIED);
        }
        return order;
    }

    private RefundVO toVO(RefundRecord record) {
        return RefundConverter.toVO(record, tradeOrderMapper.selectById(record.getOrderId()));
    }

    private static boolean hasDeposit(TradeOrder order) {
        return order.getDepositAmount() != null && order.getDepositAmount().signum() > 0;
    }

    private static String trim(String value) {
        String text = value == null ? "" : value;
        return text.length() > MAX_REMARK_LENGTH ? text.substring(0, MAX_REMARK_LENGTH) : text;
    }

    private static String generateRefundNo() {
        return "RF" + LocalDateTime.now().format(REFUND_NO_FORMATTER)
                + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 6).toUpperCase(Locale.ROOT);
    }
}
