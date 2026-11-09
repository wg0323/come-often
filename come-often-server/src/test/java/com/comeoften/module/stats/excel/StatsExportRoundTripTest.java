package com.comeoften.module.stats.excel;

import com.alibaba.excel.EasyExcel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 导出行模型必须能被 EasyExcel 原样读回：写出去是给人看的，
 * 读不回来就说明表头或数值类型写坏了，验收脚本也只能靠猜。
 */
class StatsExportRoundTripTest {

    @Test
    void should_read_back_rows_written_by_easyexcel() {
        TrendExportRow first = new TrendExportRow();
        first.setBucketStart("2026-11-03");
        first.setNetRevenue(new BigDecimal("123.45"));
        first.setRefundAmount(new BigDecimal("10.00"));
        first.setOrderCount(2L);
        first.setAvgOrderAmount(new BigDecimal("61.72"));

        TrendExportRow second = new TrendExportRow();
        second.setBucketStart("2026-11-04");
        second.setNetRevenue(BigDecimal.ZERO);
        second.setRefundAmount(BigDecimal.ZERO);
        second.setOrderCount(0L);
        second.setAvgOrderAmount(null);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output, TrendExportRow.class).sheet("营收趋势")
                .doWrite(Arrays.asList(first, second));

        List<TrendExportRow> readBack = EasyExcel.read(new ByteArrayInputStream(output.toByteArray()))
                .head(TrendExportRow.class).sheet().doReadSync();

        assertThat(readBack).hasSize(2);
        assertThat(readBack.get(0).getBucketStart()).isEqualTo("2026-11-03");
        assertThat(readBack.get(0).getNetRevenue()).isEqualByComparingTo("123.45");
        assertThat(readBack.get(0).getOrderCount()).isEqualTo(2L);
        assertThat(readBack.get(1).getBucketStart()).isEqualTo("2026-11-04");
    }

    @Test
    void should_write_header_only_when_no_rows() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output, TrendExportRow.class).sheet("营收趋势").doWrite(java.util.Collections.emptyList());

        List<TrendExportRow> readBack = EasyExcel.read(new ByteArrayInputStream(output.toByteArray()))
                .head(TrendExportRow.class).sheet().doReadSync();
        assertThat(readBack).isEmpty();

        // 空结果必须仍然带表头：按 headRowNumber(0) 读回原始行，第一行就是列名
        List<java.util.Map<Integer, String>> rawRows = EasyExcel
                .read(new ByteArrayInputStream(output.toByteArray()))
                .headRowNumber(0).sheet().doReadSync();
        assertThat(rawRows).hasSize(1);
        assertThat(rawRows.get(0)).containsValues("日期", "净营收(元)", "退款金额(元)", "订单数", "客单价(元)");
    }
}
