package com.comeoften.mq;

/**
 * RabbitMQ 拓扑常量。vhost 固定 /come-often，全部名称带 comeoften. 前缀，与其他项目隔离。
 */
public final class RabbitConst {

    public static final String VHOST = "/come-often";

    public static final String DELAY_EXCHANGE = "comeoften.delay.exchange";
    public static final String ORDER_EXCHANGE = "comeoften.order.exchange";
    public static final String MEMBER_EXCHANGE = "comeoften.member.exchange";
    public static final String DLX_EXCHANGE = "comeoften.dlx";

    public static final String ORDER_NOTIFY_QUEUE = "comeoften.order.notify.queue";
    public static final String ORDER_PAID_QUEUE = "comeoften.order.paid.queue";
    public static final String ORDER_TIMEOUT_QUEUE = "comeoften.order.timeout.queue";
    public static final String RESERVATION_TIMEOUT_QUEUE = "comeoften.reservation.timeout.queue";
    public static final String RESERVATION_REMIND_QUEUE = "comeoften.reservation.remind.queue";
    public static final String COUPON_EXPIRE_QUEUE = "comeoften.coupon.expire.queue";
    public static final String POINTS_COMPLETED_QUEUE = "comeoften.points.completed.queue";
    public static final String DEAD_QUEUE = "comeoften.dead.queue";

    public static final String RK_ORDER_NOTIFY = "order.notify";
    public static final String RK_ORDER_PAID = "order.paid";
    public static final String RK_ORDER_TIMEOUT = "order.timeout";
    public static final String RK_RESERVATION_TIMEOUT = "reservation.timeout";
    public static final String RK_RESERVATION_REMIND = "reservation.remind";
    public static final String RK_COUPON_EXPIRE = "coupon.expire";
    public static final String RK_POINTS_COMPLETED = "points.completed";
    public static final String RK_DEAD = "comeoften.dead";

    private RabbitConst() {
    }
}
