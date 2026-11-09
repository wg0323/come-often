package com.comeoften.module.stats.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 营收趋势导出行。表头与 /stats 页趋势表列一致。
 */
@Data
public class TrendExportRow {

    @ExcelProperty(value = "日期", index = 0)
    private String bucketStart;

    @ExcelProperty(value = "净营收(元)", index = 1)
    private BigDecimal netRevenue;

    @ExcelProperty(value = "退款金额(元)", index = 2)
    private BigDecimal refundAmount;

    @ExcelProperty(value = "订单数", index = 3)
    private Long orderCount;

    @ExcelProperty(value = "客单价(元)", index = 4)
    private BigDecimal avgOrderAmount;
}
