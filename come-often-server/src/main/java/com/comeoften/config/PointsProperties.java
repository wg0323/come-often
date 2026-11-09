package com.comeoften.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 积分域配置。计划 3.7 只定了「积分抵现 + expire_at 每日批处理」，未定具体额度，
 * M6 把发放额度与兑换比例做成可配置项（comeoften.points.*），不新增配置表。
 */
@Data
@Component
@ConfigurationProperties(prefix = "comeoften.points")
public class PointsProperties {

    /** 每消费 1 元获得的积分。 */
    private int orderEarnRate = 1;

    /** 多少积分抵 1 元。 */
    private int exchangeRate = 100;

    /** 每日签到积分。 */
    private int signInPoints = 5;

    /** 评价积分，M8 接入评价功能后生效。 */
    private int reviewPoints = 10;

    /** 每日分享积分。 */
    private int sharePoints = 5;

    /** 完善资料积分，终身一次。 */
    private int profilePoints = 20;

    /** 积分有效期天数，发放时写入 points_record.expire_at。 */
    private int validDays = 365;

    /** 每日过期批处理的单批条数。 */
    private int expireBatchSize = 200;
}
