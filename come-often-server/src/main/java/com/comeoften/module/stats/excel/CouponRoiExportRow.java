package com.comeoften.module.stats.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 券 ROI 导出行。
 */
@Data
public class CouponRoiExportRow {

    @ExcelProperty(value = "券模板", index = 0)
    private String templateName;

    @ExcelProperty(value = "券类型", index = 1)
    private String couponTypeText;

    @ExcelProperty(value = "发行总量", index = 2)
    private Integer total;

    @ExcelProperty(value = "领取数", index = 3)
    private Long receivedCount;

    @ExcelProperty(value = "核销数", index = 4)
    private Long usedCount;

    @ExcelProperty(value = "核销率(%)", index = 5)
    private BigDecimal useRate;

    @ExcelProperty(value = "用券订单数", index = 6)
    private Long orderCount;

    @ExcelProperty(value = "带动金额(元)", index = 7)
    private BigDecimal revenueAmount;

    @ExcelProperty(value = "折扣成本(元)", index = 8)
    private BigDecimal discountAmount;

    @ExcelProperty(value = "投产比", index = 9)
    private BigDecimal roi;
}
