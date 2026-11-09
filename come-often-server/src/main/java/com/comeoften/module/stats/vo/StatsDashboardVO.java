package com.comeoften.module.stats.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 经营看板：只回答「今天怎么样」。区间报表在 /api/admin/stats/report，
 * 与 M9 商家端 workbench/summary（积压计数 + 毛营收）不是同一个口径，刻意不复用。
 */
@Data
@Schema(description = "PC 后台经营看板")
public class StatsDashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "统计日期，yyyy-MM-dd")
    private String statDate;

    @Schema(description = "统计刷新时间，yyyy-MM-dd HH:mm:ss")
    private String updatedAt;

    @Schema(description = "今日营收汇总，净额口径")
    private StatsSummaryVO summary;

    @Schema(description = "今日预约看板")
    private StatsReservationDayVO reservations;

    @Schema(description = "今日热销排行，取前 5 条")
    private List<StatsHotDishVO> hotDishes;
}
