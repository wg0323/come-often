package com.comeoften.mq.message;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单事件消息体。只放标识与状态，金额等敏感数据不进消息体。
 * 时间用 epoch 毫秒，避免 Jackson2JsonMessageConverter 需要额外的 JSR310 模块。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderEventMessage extends EventMessage {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private Long orderId;

    private Long memberId;

    private Long storeId;

    private String fromStatus;

    private String toStatus;

    public static final String TYPE_STATUS_CHANGED = "STATUS_CHANGED";

    public static final String TYPE_TIMEOUT_CANCEL = "TIMEOUT_CANCEL";

    /** 支付成功事件，M4 发出，供 M5 发券与 M6 积分消费。 */
    public static final String TYPE_PAY_SUCCEEDED = "PAY_SUCCEEDED";
    /** 订单完成事件，M6 发出，供积分入账与等级升级消费；走 member 交换机。 */
    public static final String TYPE_ORDER_COMPLETED = "ORDER_COMPLETED";
}
