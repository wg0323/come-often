package com.comeoften.module.coupon.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 券 ROI。口径：发放量取 coupon_template.total，领取与使用取 user_coupon，
 * 用券订单与营收取 trade_order（coupon_id 关联，只统计已支付订单）。
 * 现场扫码核销的券不绑定订单，计入核销数但不计入营收。
 */
@Data
@Schema(description = "券 ROI")
public class CouponRoiVO implements Serializable {

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

    @Schema(description = "发放总量")
    private Integer total;

    @Schema(description = "剩余库存")
    private Integer remain;

    @Schema(description = "已领取数量")
    private Long receivedCount;

    @Schema(description = "已使用数量")
    private Long usedCount;

    @Schema(description = "用券订单数")
    private Long orderCount;

    @Schema(description = "用券订单累计抵扣金额，单位元")
    private BigDecimal discountAmount;

    @Schema(description = "用券订单累计实收金额，单位元")
    private BigDecimal revenueAmount;

    @Schema(description = "投产比 = 实收金额 / 抵扣金额；抵扣为零时为空")
    private BigDecimal roi;

    @Schema(description = "核销率百分比")
    private BigDecimal useRate;
}
