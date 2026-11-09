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
 * 卡包中的券。discountAmount 只是服务端给出的预估抵扣额，实际金额以下单接口返回为准。
 */
@Data
@Schema(description = "用户券")
public class UserCouponVO implements Serializable {

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

    @Schema(description = "折扣率，如 0.80")
    private BigDecimal discountRate;

    @Schema(description = "使用门槛，单位元")
    private BigDecimal thresholdAmount;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "单品券指定菜品 ID")
    private Long dishId;

    @Schema(description = "状态：0-可用，1-已使用，2-已过期")
    private Integer status;

    @Schema(description = "状态中文说明")
    private String statusText;

    @Schema(description = "是否可与会员折扣同享：0-否，1-是")
    private Integer stackable;

    @Schema(description = "面额展示文案，如「¥20」或「8折」")
    private String faceText;

    @Schema(description = "使用条件文案")
    private String conditionText;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "领取时间")
    private LocalDateTime receiveTime;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "失效时间")
    private LocalDateTime expireAt;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "核销时间")
    private LocalDateTime useTime;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "关联订单 ID")
    private Long orderId;

    @Schema(description = "当前订单场景下是否可用；仅可用券查询接口返回")
    private Boolean usable;

    @Schema(description = "不可用原因")
    private String unusableReason;

    @Schema(description = "当前订单场景下的预估抵扣额，单位元")
    private BigDecimal discountAmount;
}
