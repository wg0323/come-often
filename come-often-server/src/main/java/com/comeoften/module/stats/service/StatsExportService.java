package com.comeoften.module.stats.service;

import com.comeoften.module.stats.vo.StatsExportFile;

import java.time.LocalDate;

/**
 * 报表 Excel 导出。数据直接取 {@link StatsService#report} 的同一份结果，
 * 保证「导出行数 = 接口返回行数」，不再写第二套口径。
 */
public interface StatsExportService {

    /**
     * 同步生成 xlsx。行数超过 comeoften.stats.export-max-rows 时抛 30056，
     * 不返回被截断的文件——截断的报表比没有报表更危险。
     */
    StatsExportFile export(Long storeId, String type, LocalDate startDate, LocalDate endDate,
                          String granularity, Integer topN);
}
