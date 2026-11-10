package com.comeoften.module.bundle.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 套餐明细展示。菜名是配置时快照，菜品后续改名不影响套餐展示。
 */
@Data
@Schema(description = "团购套餐明细")
public class BundleItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "菜品 ID")
    private Long dishId;

    @Schema(description = "菜名快照")
    private String dishName;

    @Schema(description = "份数")
    private Integer quantity;

    @Schema(description = "排序值")
    private Integer sort;
}
