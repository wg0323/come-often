package com.comeoften.module.dish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 分类保存请求。顾客端展示名由后端按「预定」→「需提前预定」映射，不接受前端传入。
 */
@Data
@Schema(description = "菜品分类保存请求")
public class CategorySaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分类 ID，为空表示新增")
    private Long id;

    @NotBlank(message = "分类名称不能为空")
    @Schema(description = "分类名称；预定类固定存「预定」", requiredMode = Schema.RequiredMode.REQUIRED, example = "家常")
    private String categoryName;

    @Schema(description = "排序值")
    private Integer sort;

    @Schema(description = "是否展示", example = "true")
    private Boolean show;
}
