package com.comeoften.module.dish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 后台菜品保存请求。id 为空表示新增，非空表示整体覆盖式编辑（规格、加料、图片、标签一并提交）。
 */
@Data
@Schema(description = "菜品保存请求")
public class DishSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜品 ID，为空表示新增")
    private Long id;

    @NotBlank(message = "菜品名称不能为空")
    @Schema(description = "菜品名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "爽口青笋丝")
    private String dishName;

    @NotNull(message = "分类不能为空")
    @Schema(description = "分类 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long categoryId;

    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.00", message = "售价不能为负数")
    @Schema(description = "售价，单位元", requiredMode = Schema.RequiredMode.REQUIRED, example = "28.00")
    private BigDecimal price;

    @Schema(description = "划线价，单位元")
    private BigDecimal originalPrice;

    @Schema(description = "副标题")
    private String subtitle;

    @Schema(description = "菜品描述")
    private String description;

    @Schema(description = "食材与过敏原说明")
    private String allergen;

    @Schema(description = "技艺说明，预定菜展示该项替代月售")
    private String craftInfo;

    @Schema(description = "月售份数")
    private Integer monthlySales;

    @Schema(description = "是否为需提前预定的菜品", example = "false")
    private Boolean preorder;

    @Min(value = 0, message = "状态只允许 0 或 1")
    @Max(value = 1, message = "状态只允许 0 或 1")
    @Schema(description = "状态：0-下架，1-上架", example = "1")
    private Integer status;

    @Schema(description = "排序值")
    private Integer sort;

    @Min(value = 0, message = "备货数不能为负数")
    @Schema(description = "当日备货数，新增菜品时用于初始化每日库存，缺省 50", example = "50")
    private Integer stockTotal;

    @Valid
    @Schema(description = "规格组，验收要求支持两规格")
    private List<SpecGroupDTO> specGroups;

    @Valid
    @Schema(description = "加料，验收要求支持一加料")
    private List<AddonDTO> addons;

    @Valid
    @Schema(description = "菜品图，imageType 取 LIST / DETAIL / THUMB")
    private List<ImageDTO> images;

    @Schema(description = "标签 ID 集合")
    private List<Long> tagIds;

    /**
     * 规格组。
     */
    @Data
    @Schema(description = "规格组")
    public static class SpecGroupDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "规格组 ID，为空表示新增")
        private Long id;

        @NotBlank(message = "规格名不能为空")
        @Schema(description = "规格名，如份量", requiredMode = Schema.RequiredMode.REQUIRED)
        private String specName;

        @Schema(description = "是否必选", example = "true")
        private Boolean required;

        @Schema(description = "排序值")
        private Integer sort;

        @Valid
        @Schema(description = "规格选项")
        private List<SpecOptionDTO> options;
    }

    /**
     * 规格选项。
     */
    @Data
    @Schema(description = "规格选项")
    public static class SpecOptionDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "选项 ID，为空表示新增")
        private Long id;

        @NotBlank(message = "选项名不能为空")
        @Schema(description = "选项名，如大份", requiredMode = Schema.RequiredMode.REQUIRED)
        private String optionName;

        @DecimalMin(value = "0.00", message = "规格加价不能为负数")
        @Schema(description = "相对基础价加价，单位元", example = "10.00")
        private BigDecimal priceDelta;

        @Schema(description = "排序值")
        private Integer sort;
    }

    /**
     * 加料。
     */
    @Data
    @Schema(description = "加料")
    public static class AddonDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "加料 ID，为空表示新增")
        private Long id;

        @NotBlank(message = "加料名不能为空")
        @Schema(description = "加料名称", requiredMode = Schema.RequiredMode.REQUIRED)
        private String addonName;

        @DecimalMin(value = "0.00", message = "加料加价不能为负数")
        @Schema(description = "加价，单位元", example = "2.00")
        private BigDecimal priceDelta;
    }

    /**
     * 菜品图。
     */
    @Data
    @Schema(description = "菜品图")
    public static class ImageDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "图片 ID，为空表示新增")
        private Long id;

        @NotBlank(message = "图片对象键不能为空")
        @Schema(description = "MinIO 对象键，如 dish/1001_list.webp", requiredMode = Schema.RequiredMode.REQUIRED)
        private String objectKey;

        @Schema(description = "用途：LIST / DETAIL / THUMB / VIDEO", example = "LIST")
        private String imageType;

        @Schema(description = "轮播顺序")
        private Integer sort;
    }
}
