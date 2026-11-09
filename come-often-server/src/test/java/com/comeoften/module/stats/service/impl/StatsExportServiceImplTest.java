package com.comeoften.module.stats.service.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.stats.service.StatsService;
import com.comeoften.module.stats.vo.StatsExportFile;
import com.comeoften.module.stats.vo.StatsMemberVO;
import com.comeoften.module.stats.vo.StatsRangeVO;
import com.comeoften.module.stats.vo.StatsReportVO;
import com.comeoften.module.stats.vo.StatsTrendPointVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 导出契约的单测：行数上限的边界（300 放行 / 301 拒绝）与非法类型，
 * 这两条一旦退化，PowerShell 验收脚本只能看到「导出变慢」或「文件被截断」。
 */
@ExtendWith(MockitoExtension.class)
class StatsExportServiceImplTest {

    private static final int MAX_ROWS = 300;

    @Mock
    private StatsService statsService;

    @Mock
    private Environment environment;

    private StatsExportServiceImpl exportService;

    @BeforeEach
    void setUp() {
        exportService = new StatsExportServiceImpl(statsService, environment);
    }

    @Test
    void should_export_when_rows_equal_the_limit() {
        stubMaxRows();
        when(statsService.report(any(), any(), any(), anyString(), anyInt()))
                .thenReturn(reportWithTrendRows(MAX_ROWS));

        StatsExportFile file = exportService.export(1L, "trend", LocalDate.of(2020, 3, 2),
                LocalDate.of(2020, 3, 8), "DAY", 10);

        assertThat(file.getFileName()).contains("2020-03-02_2020-03-08").endsWith(".xlsx");
        assertThat(file.getContent()).isNotEmpty();
        assertThat(file.getContent()[0]).isEqualTo((byte) 0x50);
        assertThat(file.getContent()[1]).isEqualTo((byte) 0x4B);
    }

    @Test
    void should_reject_when_rows_exceed_the_limit() {
        stubMaxRows();
        when(statsService.report(any(), any(), any(), anyString(), anyInt()))
                .thenReturn(reportWithTrendRows(MAX_ROWS + 1));

        assertThatThrownBy(() -> exportService.export(1L, "trend", null, null, "DAY", 10))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("301")
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.STATS_EXPORT_ROW_LIMIT_EXCEEDED);
    }

    @Test
    void should_reject_unknown_export_type() {
        assertThatThrownBy(() -> exportService.export(1L, "unknown", null, null, "DAY", 10))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.PARAM_INVALID);
    }

    @Test
    void should_write_header_only_when_hot_dish_list_is_empty() {
        stubMaxRows();
        when(statsService.report(any(), any(), any(), anyString(), anyInt()))
                .thenReturn(reportWithTrendRows(0));

        StatsExportFile file = exportService.export(1L, "hot-dish", null, null, "DAY", 10);

        assertThat(file.getContent()).isNotEmpty();
        assertThat(file.getContent()[0]).isEqualTo((byte) 0x50);
    }

    @SuppressWarnings("unchecked")
    private void stubMaxRows() {
        when(environment.getProperty(eq(StatsExportServiceImpl.CONFIG_EXPORT_MAX_ROWS),
                eq(Integer.class), any())).thenReturn(MAX_ROWS);
    }

    private StatsReportVO reportWithTrendRows(int rows) {
        StatsRangeVO range = new StatsRangeVO();
        range.setStartDate("2020-03-02");
        range.setEndDate("2020-03-08");
        List<StatsTrendPointVO> trend = new ArrayList<>(rows);
        for (int index = 0; index < rows; index++) {
            StatsTrendPointVO point = new StatsTrendPointVO();
            point.setBucketStart("2020-03-" + (index + 1));
            point.setLabel("03-" + (index + 1));
            point.setNetRevenue(new BigDecimal("100.00"));
            point.setRefundAmount(BigDecimal.ZERO);
            point.setOrderCount(2L);
            point.setAvgOrderAmount(new BigDecimal("50.00"));
            trend.add(point);
        }
        StatsMemberVO member = new StatsMemberVO();
        member.setHighValueUsers(Collections.emptyList());

        StatsReportVO report = new StatsReportVO();
        report.setRange(range);
        report.setTrend(trend);
        report.setHotDishes(Collections.emptyList());
        report.setCouponRoi(Collections.emptyList());
        report.setMember(member);
        return report;
    }
}
