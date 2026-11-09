package com.comeoften.module.merchant.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商家端售罄页的单个菜品行。
 */
@Data
@Schema(description = "商家端售罄菜品")
public class SoldOutDishVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "菜品 ID")
    private Long dishId;

    @Schema(description = "菜品名称")
    private String dishName;

    @Schema(description = "售价，单位元")
    private BigDecimal price;

    @Schema(description = "当日总备货")
    private Integer total;

    @Schema(description = "当日剩余")
    private Integer remain;

    @Schema(description = "是否已售罄")
    private Boolean soldOut;

    @Schema(description = "当日是否已配置库存；未配置表示不限量")
    private Boolean configured;
}
