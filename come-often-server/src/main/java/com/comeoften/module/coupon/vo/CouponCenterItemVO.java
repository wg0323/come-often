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
 * 领券中心的一张可领券。团餐券由模板名称含「团餐」识别，首页据此单独成板块。
 */
@Data
@Schema(description = "领券中心可领券")
public class CouponCenterItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

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

    @Schema(description = "折扣率，非折扣券为空")
    private BigDecimal discountRate;

    @Schema(description = "使用门槛，单位元")
    private BigDecimal thresholdAmount;

    @Schema(description = "券面文案，如「减 30 元」")
    private String faceText;

    @Schema(description = "使用条件文案，如「满 200 元可用」")
    private String conditionText;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "可领开始时间")
    private LocalDateTime startTime;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "可领结束时间")
    private LocalDateTime endTime;

    @Schema(description = "发放总量")
    private Integer total;

    @Schema(description = "剩余库存")
    private Integer remain;

    @Schema(description = "领取后有效天数")
    private Integer validDays;

    @Schema(description = "当前会员是否已领取过")
    private Boolean received;

    @Schema(description = "当前是否可领取")
    private Boolean canReceive;

    @Schema(description = "是否团餐券（首页团餐券板块）")
    private Boolean groupMeal;
}
