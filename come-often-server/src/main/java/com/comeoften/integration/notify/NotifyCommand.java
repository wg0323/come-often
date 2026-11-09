package com.comeoften.integration.notify;

import lombok.Data;

import java.io.Serializable;

/**
 * 一条待发送的通知。通知类型与文案由业务侧决定，发送通道由 {@link NotifySender} 实现决定，
 * 换成真实订阅消息时只需要在实现里补模板映射与 openid 查询。
 */
@Data
public class NotifyCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息业务 ID，只用于日志排障，不落库。 */
    private String messageId;

    /** 通知类型，对应 message_record.message_type，如 ORDER_STATUS / MERCHANT_NEW_ORDER。 */
    private String messageType;

    private String title;

    private String content;

    private Long memberId;

    private Long storeId;
}
