package com.comeoften.module.stats.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 趋势图上的一个时间桶。按周 / 按月的桶只聚合落在查询区间内的自然日，
 * 因此各桶净营收之和恒等于区间净营收。
 */
@Data
@Schema(description = "营收趋势时间桶")
public class StatsTrendPointVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "桶起始日，yyyy-MM-dd；按周为周一，按月为当月 1 日")
    private String bucketStart;

    @Schema(description = "坐标轴展示文案：按日 MM-dd、按周 MM-dd、按月 yyyy-MM")
    private String label;

    @Schema(description = "桶内净营收，单位元")
    private BigDecimal netRevenue;

    @Schema(description = "桶内退款金额，单位元")
    private BigDecimal refundAmount;

    @Schema(description = "桶内有效订单数")
    private Long orderCount;

    @Schema(description = "桶内客单价，单位元")
    private BigDecimal avgOrderAmount;
}
