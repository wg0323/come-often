package com.comeoften.module.stats.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 内部聚合行：按支付自然日的已支付金额与有效订单数。不直接出接口，由 StatsConverter 转 VO。
 */
@Data
public class StatsDailyRevenueRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate statDate;

    private BigDecimal paidAmount;

    private Long orderCount;
}
