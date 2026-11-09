package com.comeoften.module.stats.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 内部聚合行：按菜品汇总的销量与销售额。菜名取 order_item 快照，不 join dish 取当前菜名。
 */
@Data
public class StatsHotDishRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long dishId;

    private String dishName;

    private Long quantity;

    private BigDecimal amount;

    private Long orderCount;
}
