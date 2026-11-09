package com.comeoften.module.stats.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 区间券 ROI。与 M5 的累计 ROI（/api/admin/coupon/roi）并存：
 * M5 是模板生命周期累计值，这里是按区间重算的经营活动值，两者口径同时保留。
 */
@Data
@Schema(description = "区间券 ROI")
public class StatsCouponRoiVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "券模板 ID")
    private Long templateId;

    @Schema(description = "券模板名称")
    private String templateName;

    @Schema(description = "券类型编码")
    private String couponType;

    @Schema(description = "券类型中文说明")
    private String couponTypeText;

    @Schema(description = "发行总量，模板累计值，不随区间变化")
    private Integer total;

    @Schema(description = "区间内领取数，按领取时间统计")
    private Long receivedCount;

    @Schema(description = "区间内核销数，按核销时间统计")
    private Long usedCount;

    @Schema(description = "核销率百分比 = 核销数 / 领取数，保留两位；领取数为零时为空")
    private BigDecimal useRate;

    @Schema(description = "区间内用券订单数，含整单退款单")
    private Long orderCount;

    @Schema(description = "用券订单带动金额，单位元；已扣退款（净额）")
    private BigDecimal revenueAmount;

    @Schema(description = "折扣成本 = 用券订单的优惠抵扣合计快照（trade_order.discount_amount），单位元")
    private BigDecimal discountAmount;

    @Schema(description = "投产比 = 带动金额 / 折扣成本，保留两位；成本为零时为空")
    private BigDecimal roi;
}
