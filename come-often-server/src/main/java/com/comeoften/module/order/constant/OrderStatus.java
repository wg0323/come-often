package com.comeoften.module.order.constant;

/**
 * 自提 / 到店订单状态。迁移规则集中在 {@link OrderStatusTransition}，Service 不散落 if 判断。
 */
public enum OrderStatus {

    CREATED("待支付"),
    PAID("待接单"),
    ACCEPTED("制作中"),
    READY("待取餐"),
    COMPLETED("已完成"),
    CANCELLED("已取消"),
    REFUNDING("退款审核中"),
    REFUNDED("已退款");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == REFUNDED;
    }

    /**
     * 按字符串解析，未知值返回 null，由调用方决定报错口径。
     */
    public static OrderStatus of(String value) {
        if (value == null) {
            return null;
        }
        for (OrderStatus status : values()) {
            if (status.name().equals(value)) {
                return status;
            }
        }
        return null;
    }
}
