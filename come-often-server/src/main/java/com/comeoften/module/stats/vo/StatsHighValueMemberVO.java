package com.comeoften.module.stats.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 高价值用户排行。口径是区间累计净消费（已扣已退款），不是积分余额——
 * 积分含签到、评价等非消费入账，用它排高价值用户会失真。
 */
@Data
@Schema(description = "高价值用户")
public class StatsHighValueMemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "排名，从 1 开始")
    private Integer rank;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "会员 ID")
    private Long memberId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "脱敏手机号，由服务端脱敏")
    private String phoneMasked;

    @Schema(description = "会员等级名称，未定级为空")
    private String levelName;

    @Schema(description = "区间内支付单数，含整单退款单（与净消费同源）")
    private Long orderCount;

    @Schema(description = "区间累计净消费 = 实付合计 − 退款合计，单位元")
    private BigDecimal netAmount;

    @Schema(description = "区间内最近一次支付时间")
    private LocalDateTime lastPaidTime;
}
