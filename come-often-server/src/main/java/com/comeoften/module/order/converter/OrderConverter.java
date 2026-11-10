package com.comeoften.module.order.converter;

import com.comeoften.module.dish.vo.OrderCheckVO;
import com.comeoften.module.order.constant.OrderStatus;
import com.comeoften.module.order.constant.OrderType;
import com.comeoften.module.order.entity.OrderItem;
import com.comeoften.module.order.entity.OrderStatusLog;
import com.comeoften.module.order.entity.TradeOrder;
import com.comeoften.module.order.util.OrderAmountCalculator;
import com.comeoften.module.order.vo.OrderPreviewVO;
import com.comeoften.module.order.vo.OrderSummaryVO;
import com.comeoften.module.order.vo.OrderVO;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单对象转换。金额字段保持 DB 快照原值，不做任何重算。
 */
public final class OrderConverter {

    private OrderConverter() {
    }

    public static OrderVO toVO(TradeOrder order, List<OrderItem> items, List<OrderStatusLog> logs) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderType(order.getOrderType());
        vo.setStatus(order.getStatus());
        vo.setStatusText(statusText(order.getStatus()));
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setCouponId(order.getCouponId());
        vo.setPointsAmount(order.getPointsAmount());
        vo.setPointsUsed(order.getPointsUsed());
        vo.setMemberDiscountAmount(order.getMemberDiscount());
        vo.setBalanceDeductAmount(order.getBalanceAmount());
        vo.setDepositAmount(order.getDepositAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setBalanceAmount(OrderAmountCalculator.balance(order.getPayAmount(), order.getDepositAmount()));
        boolean containsPreorder = hasDeposit(order.getDepositAmount());
        vo.setContainsPreorder(containsPreorder);
        vo.setForcedDineIn(containsPreorder && OrderType.DINE_IN.name().equals(order.getOrderType()));
        vo.setPickupCode(order.getPickupCode());
        vo.setBizDate(order.getBizDate());
        vo.setPickupTime(order.getPickupTime());
        vo.setDineInTime(order.getDineInTime());
        vo.setRemark(order.getRemark());
        vo.setPaidTime(order.getPaidTime());
        vo.setCompletedTime(order.getCompletedTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setDepositNote(containsPreorder
                ? "定金已包含在整单金额内，不退；到店付尾款，不可自提外带" : "");
        // 评价入口默认关闭，由 OrderServiceImpl 在顾客端按「已完成且未评价」回填
        vo.setCanReview(Boolean.FALSE);
        vo.setReviewed(Boolean.FALSE);
        vo.setItems(items.stream().map(OrderConverter::toItemVO).collect(Collectors.toList()));
        vo.setLogs(logs.stream().map(OrderConverter::toLogVO).collect(Collectors.toList()));
        return vo;
    }

    public static OrderSummaryVO toSummaryVO(TradeOrder order, List<OrderItem> items) {
        OrderSummaryVO vo = new OrderSummaryVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderType(order.getOrderType());
        vo.setStatus(order.getStatus());
        vo.setStatusText(statusText(order.getStatus()));
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setDepositAmount(order.getDepositAmount());
        vo.setPickupCode(order.getPickupCode());
        vo.setItemSummary(summarize(items));
        vo.setCanReview(Boolean.FALSE);
        vo.setReviewed(Boolean.FALSE);
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }

    /**
     * 确认页金额。实付 = 商品总额 − 券抵扣 − 积分抵现 − 会员折扣 − 余额抵扣，定金取实付金额的 min(200)，
     * 与下单落库口径完全一致；未用券、积分、会员折扣与余额时与 M3 的定金规则等价。
     */
    public static OrderPreviewVO toPreviewVO(OrderCheckVO check, boolean containsPreorder,
                                             OrderType orderType, boolean forcedDineIn,
                                             int preorderLeadHours, String earliestDineInTime,
                                             String depositNote, BigDecimal discountAmount,
                                             BigDecimal pointsAmount, BigDecimal memberDiscountAmount,
                                             BigDecimal balanceDeductAmount) {
        OrderPreviewVO vo = new OrderPreviewVO();
        vo.setItems(check.getItems() == null ? Collections.emptyList() : check.getItems());
        vo.setTotalAmount(check.getTotalAmount());
        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        BigDecimal points = pointsAmount == null ? BigDecimal.ZERO : pointsAmount;
        BigDecimal memberDiscount = memberDiscountAmount == null ? BigDecimal.ZERO : memberDiscountAmount;
        BigDecimal balanceDeduct = balanceDeductAmount == null ? BigDecimal.ZERO : balanceDeductAmount;
        BigDecimal pay = OrderAmountCalculator.pay(check.getTotalAmount(), discount, points,
                memberDiscount, balanceDeduct);
        BigDecimal deposit = OrderAmountCalculator.deposit(pay, containsPreorder);
        vo.setDiscountAmount(discount);
        vo.setPointsAmount(points);
        vo.setMemberDiscountAmount(memberDiscount);
        vo.setBalanceDeductAmount(balanceDeduct);
        vo.setPayAmount(pay);
        vo.setDepositAmount(deposit);
        vo.setBalanceAmount(OrderAmountCalculator.balance(pay, deposit));
        vo.setContainsPreorder(containsPreorder);
        vo.setOrderType(orderType.name());
        vo.setForcedDineIn(forcedDineIn);
        vo.setPreorderLeadHours(containsPreorder ? preorderLeadHours : 0);
        vo.setEarliestDineInTime(containsPreorder ? earliestDineInTime : "");
        vo.setDepositNote(depositNote);
        return vo;
    }

    public static String statusText(String status) {
        OrderStatus parsed = OrderStatus.of(status);
        return parsed == null ? status : parsed.getDescription();
    }

    private static OrderVO.ItemVO toItemVO(OrderItem item) {
        OrderVO.ItemVO vo = new OrderVO.ItemVO();
        vo.setDishId(item.getDishId());
        vo.setDishName(item.getDishName());
        vo.setSpecDesc(item.getSpecDesc());
        vo.setAddonDesc(item.getAddonDesc());
        vo.setPrice(item.getPrice());
        vo.setQuantity(item.getQuantity());
        vo.setAmount(item.getAmount());
        vo.setObjectKey(item.getObjectKey());
        return vo;
    }

    private static OrderVO.LogVO toLogVO(OrderStatusLog log) {
        OrderVO.LogVO vo = new OrderVO.LogVO();
        vo.setFromStatus(log.getFromStatus());
        vo.setToStatus(log.getToStatus());
        vo.setOperatorType(log.getOperatorType());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    private static String summarize(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        int count = items.stream().mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity()).sum();
        return items.get(0).getDishName() + " 等 " + count + " 件";
    }

    private static boolean hasDeposit(BigDecimal deposit) {
        return deposit != null && deposit.signum() > 0;
    }
}
