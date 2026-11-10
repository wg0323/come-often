package com.comeoften.module.dish.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Schema(description = "会员价，单位元；为空表示不设会员价")
    private BigDecimal memberPrice;

    @Schema(description = "是否仅会员可购")
    private Integer memberOnly;

    @Schema(description = "划线价，单位元")
    private BigDecimal originalPrice;

    @Schema(description = "月售份数；预定菜不展示该值")
    private Integer monthlySales;

    @Schema(description = "技艺说明，预定菜展示该项替代月售")
    private String craftInfo;

    @Schema(description = "是否需提前预定")
    private Boolean preorder;

    @Schema(description = "是否预售商品（与需提前预定不是同一功能）")
    private Boolean presale;

    @Schema(description = "预售状态：NOT_STARTED/SELLING/ENDED，非预售商品为空")
    private String presaleStatus;

    @Schema(description = "预售展示文案")
    private String presaleText;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "预售开售时间")
    private LocalDateTime presaleStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "预售结束时间")
    private LocalDateTime presaleEndTime;

    @Schema(description = "预售剩余库存，为空表示不限量")
    private Integer presaleStock;

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
