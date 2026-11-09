package com.comeoften.module.coupon.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 核销结果。成功时返回券面信息与抵扣金额，失败由业务码给出原因（已用/过期/非本店）。
 */
@Data
@Schema(description = "券核销结果")
public class CouponVerifyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

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

    @Schema(description = "面额，单位元")
    private BigDecimal faceValue;

    @Schema(description = "使用门槛，单位元")
    private BigDecimal thresholdAmount;

    @Schema(description = "本次抵扣金额，单位元")
    private BigDecimal discountAmount;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "持券会员 ID")
    private Long memberId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "核销门店 ID")
    private Long storeId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "核销员工 ID")
    private Long operatorUserId;

    @Schema(description = "核销入口：ADMIN_INPUT/MERCHANT_SCAN")
    private String verifyChannel;

    @Schema(description = "核销入口中文说明")
    private String verifyChannelText;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "核销时间")
    private LocalDateTime verifyTime;
}
