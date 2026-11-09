package com.comeoften.module.stats.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 热销排行导出行。
 */
@Data
public class HotDishExportRow {

    @ExcelProperty(value = "排名", index = 0)
    private Integer rank;

    @ExcelProperty(value = "菜品", index = 1)
    private String dishName;

    @ExcelProperty(value = "销量(份)", index = 2)
    private Long quantity;

    @ExcelProperty(value = "销售额(元)", index = 3)
    private BigDecimal amount;

    @ExcelProperty(value = "订单数", index = 4)
    private Long orderCount;
}
