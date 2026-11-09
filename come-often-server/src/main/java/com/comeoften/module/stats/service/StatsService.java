package com.comeoften.module.stats.service;

import com.comeoften.module.stats.vo.StatsDashboardVO;
import com.comeoften.module.stats.vo.StatsReportVO;

import java.time.LocalDate;

/**
 * 数据看板与报表。只读，不改任何业务状态。
 * 经营看板回答「今天怎么样」，数据报表回答「一段时间怎么样」，两个页面两套口径不合并。
 */
public interface StatsService {

    /**
     * 经营看板：今日净营收 / 订单数 / 客单价、今日预约看板、今日热销前 5。
     */
    StatsDashboardVO dashboard(Long storeId);

    /**
     * 数据报表整页。startDate / endDate 为空时默认近 7 天（含今天）；
     * granularity 为空按日；topN 为空取 10。
     */
    StatsReportVO report(Long storeId, LocalDate startDate, LocalDate endDate, String granularity,
                         Integer topN);
}
