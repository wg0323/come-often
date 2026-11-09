package com.comeoften.module.cart.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车视图。每行金额由服务端按 /dish/quote 口径返回，页面不做业务计算。
 */
@Data
@Schema(description = "购物车")
public class CartVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "购物车行")
    private List<ItemVO> items;

    @Schema(description = "有效行金额合计，单位元")
    private BigDecimal totalAmount;

    @Schema(description = "总份数")
    private Integer totalQuantity;

    @Schema(description = "是否含预定菜；含则下单强制 DINE_IN")
    private Boolean containsPreorder;

    @Schema(description = "整单定金预估，单位元")
    private BigDecimal depositAmount;

    /**
     * 购物车行。
     */
    @Data
    @Schema(description = "购物车行")
    public static class ItemVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "购物车行 ID")
        private Long itemId;

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "菜品 ID")
        private Long dishId;

        @Schema(description = "菜品名称")
        private String dishName;

        @Schema(description = "列表图 objectKey")
        private String objectKey;

        @Schema(description = "规格描述快照")
        private String specDesc;

        @Schema(description = "加料描述快照")
        private String addonDesc;

        @JsonSerialize(contentUsing = ToStringSerializer.class)
        @Schema(description = "已选规格选项 ID，供确认订单页回传")
        private List<Long> specOptionIds;

        @JsonSerialize(contentUsing = ToStringSerializer.class)
        @Schema(description = "已选加料 ID，供确认订单页回传")
        private List<Long> addonIds;

        @Schema(description = "服务端试算单位价，单位元")
        private BigDecimal unitPrice;

        @Schema(description = "份数")
        private Integer quantity;

        @Schema(description = "服务端试算行金额，单位元")
        private BigDecimal amount;

        @Schema(description = "是否预定菜")
        private Boolean preorder;

        @Schema(description = "菜品是否已失效（下架 / 规格变更 / 售罄）")
        private Boolean invalid;

        @Schema(description = "失效原因")
        private String invalidReason;
    }
}
