package com.comeoften.mq.message;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 券即将过期提醒消息体。领券时按 expire_at 减提前量投递到 comeoften.coupon.expire.queue，
 * 由延迟交换机到点触发；M11 的每小时定时任务作为兜底。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CouponExpireMessage extends EventMessage {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_EXPIRE_REMIND = "COUPON_EXPIRE_REMIND";

    private Long userCouponId;

    private Long templateId;

    private String templateName;

    private Long memberId;

    private Long storeId;

    /** 券失效时间，epoch 毫秒。 */
    private long expireAtMillis;
}
