package com.comeoften.module.coupon.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PC 后台券模板视图。雪花 ID 一律序列化成字符串，避免 JS 精度丢失。
 */
@Data
@Schema(description = "券模板")
public class CouponTemplateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "券模板 ID")
    private Long id;

    @Schema(description = "券模板名称")
    private String templateName;

    @Schema(description = "券类型编码：FULL_REDUCTION/DISCOUNT/DISH/NO_THRESHOLD/BIRTHDAY")
    private String couponType;

    @Schema(description = "券类型中文说明")
    private String couponTypeText;

    @Schema(description = "面额，单位元")
    private BigDecimal faceValue;

    @Schema(description = "折扣率，如 0.80；非折扣券为空")
    private BigDecimal discountRate;

    @Schema(description = "使用门槛，单位元")
    private BigDecimal thresholdAmount;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "单品券指定菜品 ID")
    private Long dishId;

    @Schema(description = "单品券适用菜品 ID 集合")
    private List<String> dishIds;

    @Schema(description = "发放总量")
    private Integer total;

    @Schema(description = "剩余库存")
    private Integer remain;

    @Schema(description = "领取后有效天数")
    private Integer validDays;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "可领开始时间")
    private LocalDateTime startTime;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "可领结束时间")
    private LocalDateTime endTime;

    @Schema(description = "是否可与会员折扣同享：0-否，1-是")
    private Integer stackable;

    @Schema(description = "状态：0-草稿，1-发放中，2-已停用")
    private Integer status;

    @Schema(description = "状态中文说明")
    private String statusText;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
