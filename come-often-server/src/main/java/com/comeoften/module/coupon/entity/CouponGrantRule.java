package com.comeoften.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 券发放规则。{@code uk_store_rule(store_id, rule_code)} 不含 is_deleted，
 * 保存时按 rule_code 先查后改并复活，不直接 insert（见 deploy/init.md 差异 631）。
 */
@Data
@TableName("coupon_grant_rule")
public class CouponGrantRule implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long storeId;

    private String ruleCode;

    private String ruleName;

    /** NEW_MEMBER / BIRTHDAY / REPEAT_PURCHASE / SLEEPING。 */
    private String ruleType;

    private Long couponTemplateId;

    /** 阈值：复购按已支付订单数，沉睡按天数；新客与生日忽略。 */
    private Integer triggerValue;

    private Integer maxPerMember;

    private Integer isEnabled;

    private String remark;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
