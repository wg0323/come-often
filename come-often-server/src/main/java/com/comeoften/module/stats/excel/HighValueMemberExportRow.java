package com.comeoften.module.stats.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 高价值用户导出行。时间用字符串输出，避免 Excel 单元格被识别成数值序列号。
 */
@Data
public class HighValueMemberExportRow {

    @ExcelProperty(value = "排名", index = 0)
    private Integer rank;

    @ExcelProperty(value = "昵称", index = 1)
    private String nickname;

    @ExcelProperty(value = "手机号", index = 2)
    private String phoneMasked;

    @ExcelProperty(value = "会员等级", index = 3)
    private String levelName;

    @ExcelProperty(value = "订单数", index = 4)
    private Long orderCount;

    @ExcelProperty(value = "净消费(元)", index = 5)
    private BigDecimal netAmount;

    @ExcelProperty(value = "最近消费时间", index = 6)
    private String lastPaidTime;
}
