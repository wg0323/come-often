package com.comeoften.mq.message;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预约事件消息体。超时未签到与提前提醒共用，靠 eventType 区分；
 * reserveAtMillis 是投递时的预约时刻，改期后旧消息到点可据此丢弃，不需要额外表记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReservationMessage extends EventMessage {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_TIMEOUT_NO_SHOW = "RESERVATION_TIMEOUT_NO_SHOW";

    public static final String TYPE_REMIND = "RESERVATION_REMIND";

    private String reserveNo;

    private Long reservationId;

    private Long memberId;

    private Long storeId;

    private String reserveDate;

    private String timeSlot;

    private String tableType;

    /** 预约时刻，epoch 毫秒。消费者比对当前预约时刻，改期后旧消息直接丢弃。 */
    private long reserveAtMillis;
}
