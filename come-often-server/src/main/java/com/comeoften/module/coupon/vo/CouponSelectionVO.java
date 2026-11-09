package com.comeoften.module.coupon.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 下单时的券选择结果。金额由 CouponDiscountCalculator 在服务端算出，页面只展示。
 */
@Data
@Schema(description = "下单用券结果")
public class CouponSelectionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "用户券 ID")
    private Long userCouponId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "券模板 ID")
    private Long templateId;

    @Schema(description = "券模板名称")
    private String templateName;

    @Schema(description = "券类型编码")
    private String couponType;

    @Schema(description = "券类型中文说明")
    private String couponTypeText;

    @Schema(description = "抵扣金额，单位元")
    private BigDecimal discountAmount;
}
