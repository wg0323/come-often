package com.comeoften.module.dish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 顾客端菜品列表查询条件。
 */
@Data
@Schema(description = "菜品列表查询条件")
public class DishQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分类 ID，为空表示全部上架菜品")
    private Long categoryId;

    @Schema(description = "关键词，匹配菜名、副标题与描述")
    private String keyword;

    @Schema(description = "只查预定菜")
    private Boolean preorder;

    @Schema(description = "门店 ID，为空回退到当前唯一门店")
    private Long storeId;

    @Schema(description = "只查招牌菜")
    private Boolean signature;
}
