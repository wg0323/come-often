package com.comeoften.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 会员运营配置。
 *
 * <p>沉睡阈值走配置（M11 没做沉睡打标的根因就是「阈值没定」）：默认 30 天无支付成功订单即视为沉睡，
 * PC 后台会员列表可按标签筛选，券发放规则 {@code SLEEPING} 也读同一阈值口径。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "comeoften.member")
public class MemberProperties {

    /** 连续多少天没有支付成功订单视为沉睡会员。 */
    private int sleepingDays = 30;

    /** 沉睡标签扫描的单批条数。 */
    private int tagBatchSize = 200;
}
