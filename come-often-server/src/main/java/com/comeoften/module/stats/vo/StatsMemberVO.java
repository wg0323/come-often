package com.comeoften.module.stats.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 会员留存与复购。复购率分母是「区间内有支付单的会员」，不是全部注册会员，
 * 否则大量注册未消费的会员会把复购率稀释到看不出经营变化。
 */
@Data
@Schema(description = "会员复购与留存")
public class StatsMemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "区间内有支付单的会员数，复购率的分母")
    private Long paidMemberCount;

    @Schema(description = "区间内支付两单及以上的会员数，复购率的分子")
    private Long repurchaseMemberCount;

    @Schema(description = "复购率百分比，保留两位；分母为零时为空")
    private BigDecimal repurchaseRate;

    @Schema(description = "次日留存考察的新客数：区间内首单且次日已完整结束的会员")
    private Long retentionCohortSize;

    @Schema(description = "上述新客中次日再次支付的会员数")
    private Long retentionCount;

    @Schema(description = "次日留存率百分比，保留两位；分母为零时为空")
    private BigDecimal retentionRate;

    @Schema(description = "高价值用户排行，按区间累计净消费降序")
    private List<StatsHighValueMemberVO> highValueUsers;
}
