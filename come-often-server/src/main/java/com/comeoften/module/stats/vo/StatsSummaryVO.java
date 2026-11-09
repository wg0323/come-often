package com.comeoften.module.stats.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 区间营收汇总。净营收已扣退款，订单数与客单价同源，三者保证互相能对上。
 */
@Data
@Schema(description = "区间营收汇总")
public class StatsSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "净营收 = 已支付实付金额合计 − 已退款金额合计，单位元")
    private BigDecimal netRevenue;

    @Schema(description = "退款金额合计，单位元；按原订单支付时间冲减")
    private BigDecimal refundAmount;

    @Schema(description = "有效订单数，不含已取消与整单已退款订单")
    private Long orderCount;

    @Schema(description = "客单价 = 净营收 / 有效订单数，单位元，保留两位")
    private BigDecimal avgOrderAmount;
}
