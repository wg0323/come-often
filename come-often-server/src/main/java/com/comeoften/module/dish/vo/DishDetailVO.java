package com.comeoften.module.dish.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 菜品详情。顾客端与后台编辑共用；预定菜额外返回铜金角标文案与 200 元定金说明。
 */
@Data
@Schema(description = "菜品详情")
public class DishDetailVO implements Serializable {

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

    @Schema(description = "菜品描述")
    private String description;

    @Schema(description = "售价，单位元")
    private BigDecimal price;

    @Schema(description = "会员价，单位元；为空表示不设会员价")
    private BigDecimal memberPrice;

    @Schema(description = "是否仅会员可购")
    private Integer memberOnly;

    @Schema(description = "划线价，单位元")
    private BigDecimal originalPrice;

    @Schema(description = "月售份数；预定菜不展示")
    private Integer monthlySales;

    @Schema(description = "食材与过敏原说明")
    private String allergen;

    @Schema(description = "技艺说明，预定菜展示该项替代月售")
    private String craftInfo;

    @Schema(description = "状态：0-下架，1-上架")
    private Integer status;

    @Schema(description = "排序值")
    private Integer sort;

    @Schema(description = "是否需提前预定")
    private Boolean preorder;

    @Schema(description = "需提前预订小时数，固定 24")
    private Integer preorderHours;

    @Schema(description = "预定菜角标文案", example = "需提前 1 天预定")
    private String preorderBadge;

    @Schema(description = "预定菜常驻说明", example = "付 200 元定金锁定，定金不退，仅限到店用餐")
    private String preorderNote;

    @Schema(description = "整单定金，单位元", example = "200.00")
    private BigDecimal depositAmount;

    @Schema(description = "预定菜是否仅限到店用餐")
    private Boolean onlyDineIn;

    @Schema(description = "是否今日售罄")
    private Boolean soldOut;

    @Schema(description = "当日剩余库存")
    private Integer stockRemain;

    @Schema(description = "评价平均分，一位小数；无评价为 0.0", example = "4.5")
    private BigDecimal ratingAverage;

    @Schema(description = "评价条数", example = "12")
    private Long reviewCount;

    @Schema(description = "菜品图")
    private List<ImageVO> images;

    @Schema(description = "规格组")
    private List<SpecGroupVO> specGroups;

    @Schema(description = "加料")
    private List<AddonVO> addons;

    @Schema(description = "标签名集合")
    private List<String> tags;

    @JsonSerialize(contentUsing = ToStringSerializer.class)
    @Schema(description = "标签 ID 集合，供后台回显")
    private List<Long> tagIds;

    /**
     * 菜品图。
     */
    @Data
    @Schema(description = "菜品图")
    public static class ImageVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "图片 ID")
        private Long id;

        @Schema(description = "MinIO 对象键")
        private String objectKey;

        @Schema(description = "用途：LIST / DETAIL / THUMB / VIDEO")
        private String imageType;

        @Schema(description = "轮播顺序")
        private Integer sort;
    }

    /**
     * 规格组。
     */
    @Data
    @Schema(description = "规格组")
    public static class SpecGroupVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "规格组 ID")
        private Long id;

        @Schema(description = "规格名")
        private String specName;

        @Schema(description = "是否必选")
        private Boolean required;

        @Schema(description = "规格选项")
        private List<SpecOptionVO> options;
    }

    /**
     * 规格选项。
     */
    @Data
    @Schema(description = "规格选项")
    public static class SpecOptionVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "选项 ID")
        private Long id;

        @Schema(description = "选项名")
        private String optionName;

        @Schema(description = "相对基础价加价，单位元")
        private BigDecimal priceDelta;
    }

    /**
     * 加料。
     */
    @Data
    @Schema(description = "加料")
    public static class AddonVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "加料 ID")
        private Long id;

        @Schema(description = "加料名称")
        private String addonName;

        @Schema(description = "加价，单位元")
        private BigDecimal priceDelta;
    }
}
