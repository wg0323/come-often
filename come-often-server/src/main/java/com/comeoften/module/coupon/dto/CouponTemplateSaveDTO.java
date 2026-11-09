package com.comeoften.module.coupon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 券模板新建 / 编辑请求。面额与门槛一律由后台录入、服务端落库，不允许前端算抵扣。
 */
@Data
@Schema(description = "券模板保存请求")
public class CouponTemplateSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @NotBlank(message = "券名称不能为空")
    @Size(max = 128, message = "券名称最多 128 字")
    @Schema(description = "券模板名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String templateName;

    @NotBlank(message = "券类型不能为空")
    @Schema(description = "券类型：FULL_REDUCTION/DISCOUNT/DISH/NO_THRESHOLD/BIRTHDAY",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String couponType;

    @DecimalMin(value = "0.00", message = "面额不能为负")
    @Schema(description = "面额，单位元；折扣券可不填")
    private BigDecimal faceValue;

    @DecimalMin(value = "0.01", message = "折扣率不合法")
    @DecimalMax(value = "1.00", message = "折扣率不能大于 1.00")
    @Schema(description = "折扣率，如 0.80；仅折扣券必填")
    private BigDecimal discountRate;

    @DecimalMin(value = "0.00", message = "门槛不能为负")
    @Schema(description = "使用门槛，单位元")
    private BigDecimal thresholdAmount;

    @Schema(description = "单品券指定菜品 ID")
    private Long dishId;

    @Schema(description = "单品券适用菜品 ID 集合，可与 dishId 二选一")
    private List<Long> dishIds;

    @NotNull(message = "发放总量不能为空")
    @Min(value = 1, message = "发放总量至少为 1")
    @Schema(description = "发放总量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer total;

    @Min(value = 1, message = "有效天数至少为 1")
    @Schema(description = "领取后有效天数", example = "30")
    private Integer validDays;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "可领开始时间")
    private LocalDateTime startTime;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "可领结束时间")
    private LocalDateTime endTime;

    @Schema(description = "是否可与会员折扣同享：0-否，1-是")
    private Integer stackable;

    @Schema(description = "状态：0-草稿，1-发放中，2-已停用；新建默认 1")
    private Integer status;
}
