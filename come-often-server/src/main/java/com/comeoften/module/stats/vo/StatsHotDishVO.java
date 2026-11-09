package com.comeoften.module.stats.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 热销排行一行。按菜品聚合，同一菜品的不同规格合并；金额取 order_item.amount 下单快照合计（毛额）。
 */
@Data
@Schema(description = "热销排行")
public class StatsHotDishVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "排名，从 1 开始")
    private Integer rank;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "菜品 ID")
    private Long dishId;

    @Schema(description = "菜名，取下单快照中的名称")
    private String dishName;

    @Schema(description = "销量（份）")
    private Long quantity;

    @Schema(description = "销售额，单位元；菜品行小计快照合计，未按退款摊分")
    private BigDecimal amount;

    @Schema(description = "出现该菜品的有效订单数")
    private Long orderCount;
}
