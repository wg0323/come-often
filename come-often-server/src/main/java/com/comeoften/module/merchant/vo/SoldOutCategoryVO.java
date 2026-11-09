package com.comeoften.module.merchant.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 商家端售罄页的分类分组，页面按分类折叠展示。
 */
@Data
@Schema(description = "商家端售罄分类分组")
public class SoldOutCategoryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "分类 ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "该分类下的上架菜品")
    private List<SoldOutDishVO> dishes;
}
