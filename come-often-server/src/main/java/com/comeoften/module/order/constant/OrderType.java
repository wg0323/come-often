package com.comeoften.module.order.constant;

/**
 * 订单类型。含预定菜的订单由服务端强制为 DINE_IN，前端传值不参与判定。
 */
public enum OrderType {

    PICKUP("自提"),
    DINE_IN("到店用餐");

    private final String description;

    OrderType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static OrderType of(String value) {
        if (value == null) {
            return null;
        }
        for (OrderType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        return null;
    }
}
