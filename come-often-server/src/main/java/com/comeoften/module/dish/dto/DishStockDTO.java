package com.comeoften.module.dish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 后台每日库存设置请求。soldOut 为 true 时直接置余量为 0 并同步 Redis。
 */
@Data
@Schema(description = "菜品每日库存设置请求")
public class DishStockDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "菜品 ID 不能为空")
    @Schema(description = "菜品 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long dishId;

    @Min(value = 0, message = "备货数不能为负数")
    @Schema(description = "当日备货数", example = "50")
    private Integer total;

    @Schema(description = "是否一键售罄；为 true 时忽略 total，余量置 0", example = "false")
    private Boolean soldOut;
}
