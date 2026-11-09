package com.comeoften.module.dish.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 每日库存视图，DB 的 remain 为最终事实源。
 */
@Data
@Schema(description = "菜品每日库存")
public class DishStockVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "菜品 ID")
    private Long dishId;

    @Schema(description = "菜品名称")
    private String dishName;

    @Schema(description = "库存日期 yyyy-MM-dd")
    private String stockDate;

    @Schema(description = "当日总备货")
    private Integer total;

    @Schema(description = "当日剩余")
    private Integer remain;

    @Schema(description = "是否售罄")
    private Boolean soldOut;

    @Schema(description = "当日是否已配置库存；未配置表示不限量")
    private Boolean configured;
}
