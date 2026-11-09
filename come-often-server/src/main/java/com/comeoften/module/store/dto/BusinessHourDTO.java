package com.comeoften.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 单日营业时间。时间用 HH:mm 字符串，避免前端时区与秒数格式差异。
 */
@Data
@Schema(description = "单日营业时间")
public class BusinessHourDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "星期需为 1 到 7")
    @Max(value = 7, message = "星期需为 1 到 7")
    @Schema(description = "星期：1-周一，7-周日", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer weekDay;

    @NotBlank(message = "开始时间不能为空")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "开始时间格式需为 HH:mm")
    @Schema(description = "开始营业时间 HH:mm", requiredMode = Schema.RequiredMode.REQUIRED, example = "10:00")
    private String openTime;

    @NotBlank(message = "结束时间不能为空")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "结束时间格式需为 HH:mm")
    @Schema(description = "结束营业时间 HH:mm", requiredMode = Schema.RequiredMode.REQUIRED, example = "21:30")
    private String closeTime;
}
