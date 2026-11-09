package com.comeoften.module.dish.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 分类页与后台列表共用的菜品摘要。金额单位元，前端只做展示不再计算。
 */
@Data
@Schema(description = "菜品列表项")
public class DishListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "菜品 ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "分类 ID")
    private Long categoryId;

    @Schema(description = "分类展示名")
    private String categoryName;

    @Schema(description = "菜品名称")
    private String dishName;

    @Schema(description = "副标题")
    private String subtitle;

    @Schema(description = "售价，单位元")
    private BigDecimal price;

    @Schema(description = "划线价，单位元")
    private BigDecimal originalPrice;

    @Schema(description = "月售份数；预定菜不展示该值")
    private Integer monthlySales;

    @Schema(description = "技艺说明，预定菜展示该项替代月售")
    private String craftInfo;

    @Schema(description = "是否需提前预定")
    private Boolean preorder;

    @Schema(description = "是否今日售罄")
    private Boolean soldOut;

    @Schema(description = "状态：0-下架，1-上架")
    private Integer status;

    @Schema(description = "排序值")
    private Integer sort;

    @Schema(description = "列表图 objectKey，比例 4:3")
    private String listImage;

    @Schema(description = "标签名集合")
    private List<String> tags;
}
