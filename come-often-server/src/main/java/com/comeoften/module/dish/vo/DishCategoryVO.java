package com.comeoften.module.dish.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 分类展示对象。预定类在顾客端显示「需提前预定」。
 */
@Data
@Schema(description = "菜品分类")
public class DishCategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "分类 ID")
    private Long id;

    @Schema(description = "顾客端展示名", example = "需提前预定")
    private String categoryName;

    @Schema(description = "后台存储名", example = "预定")
    private String storageName;

    @Schema(description = "排序值")
    private Integer sort;

    @Schema(description = "该分类下上架菜品数")
    private Long dishCount;

    @Schema(description = "是否预定分类")
    private Boolean preorder;
}
