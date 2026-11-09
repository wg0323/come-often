package com.comeoften.module.order.constant;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态机迁移表。允许的 from→to 集合是唯一真相源，非法迁移抛 ORDER_STATUS_INVALID。
 *
 * <p>计划 3.3：CREATED→PAID→ACCEPTED→READY→COMPLETED 为主链；
 * 待支付超时或用户取消进 CANCELLED；已接单后的退款走商家审核 REFUNDING→REFUNDED。</p>
 */
public final class OrderStatusTransition {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED;

    static {
        Map<OrderStatus, Set<OrderStatus>> table = new EnumMap<>(OrderStatus.class);
        table.put(OrderStatus.CREATED, EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED));
        table.put(OrderStatus.PAID,
                EnumSet.of(OrderStatus.ACCEPTED, OrderStatus.CANCELLED, OrderStatus.REFUNDING));
        table.put(OrderStatus.ACCEPTED, EnumSet.of(OrderStatus.READY, OrderStatus.REFUNDING));
        table.put(OrderStatus.READY, EnumSet.of(OrderStatus.COMPLETED, OrderStatus.REFUNDING));
        table.put(OrderStatus.REFUNDING, EnumSet.of(OrderStatus.REFUNDED, OrderStatus.ACCEPTED));
        table.put(OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class));
        table.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        table.put(OrderStatus.REFUNDED, EnumSet.noneOf(OrderStatus.class));
        Map<OrderStatus, Set<OrderStatus>> immutable = new EnumMap<>(OrderStatus.class);
        table.forEach((status, targets) ->
                immutable.put(status, Collections.unmodifiableSet(EnumSet.copyOf(targets))));
        ALLOWED = Collections.unmodifiableMap(immutable);
    }

    private OrderStatusTransition() {
    }

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            return false;
        }
        Set<OrderStatus> targets = ALLOWED.get(from);
        return targets != null && targets.contains(to);
    }

    /**
     * 断言允许迁移；非法迁移抛 30002，错误文案带 from→to 便于排查。
     */
    public static void assertAllowed(OrderStatus from, OrderStatus to) {
        if (!canTransition(from, to)) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID,
                    "订单状态流转非法：" + (from == null ? "null" : from.name())
                            + " → " + (to == null ? "null" : to.name()));
        }
    }

    public static Set<OrderStatus> targetsOf(OrderStatus from) {
        Set<OrderStatus> targets = ALLOWED.get(from);
        return targets == null ? Collections.emptySet() : targets;
    }
}
