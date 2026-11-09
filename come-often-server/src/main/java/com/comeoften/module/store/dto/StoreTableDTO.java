package com.comeoften.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 桌台保存请求。桌号与容量全部由数据配置，代码不写死。
 */
@Data
@Schema(description = "桌台保存请求")
public class StoreTableDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "桌台 ID，为空表示新增")
    private Long id;

    @NotBlank(message = "桌号不能为空")
    @Schema(description = "桌号或包间名", requiredMode = Schema.RequiredMode.REQUIRED, example = "A01")
    private String tableNo;

    @NotBlank(message = "桌型不能为空")
    @Schema(description = "桌型：HALL_CARD / PRIVATE_SMALL / PRIVATE_LARGE / WINDOW", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tableType;

    @Min(value = 1, message = "容纳人数需大于 0")
    @Schema(description = "可容纳人数", requiredMode = Schema.RequiredMode.REQUIRED, example = "4")
    private Integer capacity;

    @Schema(description = "状态：0-空闲，1-占用，2-已预约", example = "0")
    private Integer status;
}
