package com.comeoften.module.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 商家端桌台状态变更请求。只允许改状态，桌号与容量留在 PC 后台。
 */
@Data
@Schema(description = "桌台状态变更请求")
public class TableStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "桌台状态不能为空")
    @Min(value = 0, message = "桌台状态只能是 0 / 1 / 2")
    @Max(value = 2, message = "桌台状态只能是 0 / 1 / 2")
    @Schema(description = "状态：0-空闲，1-占用，2-已预约", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "1")
    private Integer status;
}
