package com.comeoften.module.stats.service.impl;

import com.alibaba.excel.EasyExcel;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.stats.constant.StatsExportType;
import com.comeoften.module.stats.converter.StatsConverter;
import com.comeoften.module.stats.excel.CouponRoiExportRow;
import com.comeoften.module.stats.excel.HighValueMemberExportRow;
import com.comeoften.module.stats.excel.HotDishExportRow;
import com.comeoften.module.stats.excel.TrendExportRow;
import com.comeoften.module.stats.service.StatsExportService;
import com.comeoften.module.stats.service.StatsService;
import com.comeoften.module.stats.vo.StatsExportFile;
import com.comeoften.module.stats.vo.StatsReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 报表导出实现。数据源就是 {@link StatsService#report} 的同一份结果，
 * 因此导出必然与页面数字一致；行数上限先判后写，绝不产出被截断的文件。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsExportServiceImpl implements StatsExportService {

    /** 单次导出行数上限，超过返回 30056。默认 300 行，按日趋势约 10 个月。 */
    static final String CONFIG_EXPORT_MAX_ROWS = "comeoften.stats.export-max-rows";

    private static final int DEFAULT_MAX_ROWS = 300;

    private static final String CONTENT_SUFFIX = ".xlsx";

    private final StatsService statsService;

    private final Environment environment;

    @Override
    public StatsExportFile export(Long storeId, String type, LocalDate startDate, LocalDate endDate,
                                  String granularity, Integer topN) {
        StatsExportType exportType = StatsExportType.of(type);
        if (exportType == null) {
            throw new BizException(ErrorCode.PARAM_INVALID,
                    "导出类型只支持 trend / hot-dish / coupon-roi / high-value-member");
        }
        StatsReportVO report = statsService.report(storeId, startDate, endDate, granularity, topN);
        int maxRows = environment.getProperty(CONFIG_EXPORT_MAX_ROWS, Integer.class, DEFAULT_MAX_ROWS);
        byte[] content = buildContent(exportType, report, maxRows);
        String fileName = "报表-" + exportType.getFileLabel() + "-" + report.getRange().getStartDate()
                + "_" + report.getRange().getEndDate() + CONTENT_SUFFIX;
        log.info("报表导出完成: storeId={}, type={}, startDate={}, endDate={}", storeId,
                exportType.getCode(), report.getRange().getStartDate(), report.getRange().getEndDate());
        return StatsExportFile.of(fileName, content);
    }

    private byte[] buildContent(StatsExportType type, StatsReportVO report, int maxRows) {
        switch (type) {
            case TREND:
                return writeSheet(type, StatsConverter.toTrendExportRows(report.getTrend()), maxRows);
            case HOT_DISH:
                return writeSheet(type, StatsConverter.toHotDishExportRows(report.getHotDishes()), maxRows);
            case COUPON_ROI:
                return writeSheet(type, StatsConverter.toCouponRoiExportRows(report.getCouponRoi()), maxRows);
            case HIGH_VALUE_MEMBER:
                return writeSheet(type, StatsConverter.toHighValueMemberExportRows(
                        report.getMember() == null ? null : report.getMember().getHighValueUsers()), maxRows);
            default:
                throw new BizException(ErrorCode.PARAM_INVALID, "不支持的导出类型：" + type.getCode());
        }
    }

    /**
     * 空结果也要出表头：按类型固定导出行类型，不依赖数据里第一条记录的实际类型。
     */
    private <T> byte[] writeSheet(StatsExportType type, List<T> rows, int maxRows) {
        List<T> data = rows == null ? Collections.emptyList() : rows;
        if (data.size() > maxRows) {
            throw new BizException(ErrorCode.STATS_EXPORT_ROW_LIMIT_EXCEEDED,
                    "导出数据 " + data.size() + " 行，超过上限 " + maxRows + " 行，请缩小查询区间");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output, rowClassOf(type)).sheet(type.getSheetName()).doWrite(data);
        return output.toByteArray();
    }

    private Class<?> rowClassOf(StatsExportType type) {
        switch (type) {
            case TREND:
                return TrendExportRow.class;
            case HOT_DISH:
                return HotDishExportRow.class;
            case COUPON_ROI:
                return CouponRoiExportRow.class;
            case HIGH_VALUE_MEMBER:
                return HighValueMemberExportRow.class;
            default:
                return Object.class;
        }
    }
}
