package com.comeoften.module.merchant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商家端工作台概览。三个订单数字与订单看板同源（PAID / ACCEPTED / READY），
 * 今日营收取当日已支付的实付金额汇总。
 */
@Data
@Schema(description = "商家端工作台概览")
public class WorkbenchSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "待接单数（PAID）")
    private Long pendingAcceptCount;

    @Schema(description = "制作中数量（ACCEPTED）")
    private Long cookingCount;

    @Schema(description = "待取餐数量（READY）")
    private Long readyCount;

    @Schema(description = "今日预约数，不含已取消")
    private Long todayReservationCount;

    @Schema(description = "今日营收，单位元；按当日已支付的实付金额汇总")
    private BigDecimal todayRevenue;
}
