package com.comeoften.module.points.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 积分兑换菜品请求。只传「换哪道菜、几份、选了什么规格」，所需积分由服务端按菜品金额换算。
 */
@Data
@Schema(description = "积分兑换菜品请求")
public class PointsExchangeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "菜品 ID 不能为空")
    @Schema(description = "菜品 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long dishId;

    @Min(value = 1, message = "份数必须大于 0")
    @Schema(description = "份数", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer quantity;

    @Schema(description = "已选规格选项 ID（每组至多一项）")
    private List<Long> specOptionIds;

    @Schema(description = "已选加料 ID")
    private List<Long> addonIds;
}
