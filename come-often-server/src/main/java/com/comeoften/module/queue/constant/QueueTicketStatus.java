package com.comeoften.module.queue.constant;

/**
 * 排队号状态。与 queue_ticket.status 一一对应，跨天不再复用的号由运营侧置 EXPIRED。
 */
public enum QueueTicketStatus {

    WAITING("等待中"),

    CALLED("已叫号"),

    SEATED("已入座"),

    CANCELLED("已取消"),

    EXPIRED("已过号");

    private final String description;

    QueueTicketStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static String describe(String code) {
        if (code == null) {
            return "";
        }
        for (QueueTicketStatus status : values()) {
            if (status.name().equals(code)) {
                return status.getDescription();
            }
        }
        return code;
    }
}
