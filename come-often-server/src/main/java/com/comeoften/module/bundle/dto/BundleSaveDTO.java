package com.comeoften.module.bundle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 后台保存团购套餐。整体覆盖式编辑：明细列表每次全量提交，服务端按菜品查快照重建。
 */
@Data
@Schema(description = "团购套餐保存请求")
public class BundleSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "套餐 ID，为空表示新增")
    private Long id;

    @NotBlank(message = "套餐名称不能为空")
    @Size(max = 128, message = "套餐名称不超过 128 字")
    @Schema(description = "套餐名称", required = true, example = "双人尝鲜套餐")
    private String bundleName;

    @Size(max = 500, message = "套餐说明不超过 500 字")
    @Schema(description = "套餐说明")
    private String description;

    @NotNull(message = "套餐售价不能为空")
    @DecimalMin(value = "0.01", message = "套餐售价必须大于 0")
    @Schema(description = "套餐售价，单位元", required = true, example = "88.00")
    private BigDecimal bundlePrice;

    @Schema(description = "划线价，单位元", example = "128.00")
    private BigDecimal originalPrice;

    @NotNull(message = "套餐总库存不能为空")
    @Min(value = 0, message = "套餐总库存不能为负数")
    @Schema(description = "套餐总库存", required = true, example = "50")
    private Integer totalStock;

    @Min(value = 0, message = "状态只允许 0 或 1")
    @Max(value = 1, message = "状态只允许 0 或 1")
    @Schema(description = "状态：0-下架，1-上架", example = "1")
    private Integer status;

    @Valid
    @NotEmpty(message = "套餐明细不能为空")
    @Schema(description = "套餐明细，至少一道菜", required = true)
    private List<ItemDTO> items;

    /**
     * 套餐内的一道菜。
     */
    @Data
    @Schema(description = "套餐明细项")
    public static class ItemDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @NotNull(message = "菜品 ID 不能为空")
        @Schema(description = "菜品 ID", required = true)
        private Long dishId;

        @Min(value = 1, message = "份数至少 1 份")
        @Max(value = 99, message = "单菜最多 99 份")
        @Schema(description = "份数，默认 1", example = "1")
        private Integer quantity;
    }
}
