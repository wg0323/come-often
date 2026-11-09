package com.comeoften.module.stats.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 数据报表整页。刻意用一个聚合接口返回整页：首屏只有一个请求，
 * 切粒度 / 换区间只刷一次，避免五张卡片并发打库。
 */
@Data
@Schema(description = "数据报表整页")
public class StatsReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "实际生效的查询区间与粒度")
    private StatsRangeVO range;

    @Schema(description = "区间营收汇总")
    private StatsSummaryVO summary;

    @Schema(description = "营收与订单趋势")
    private List<StatsTrendPointVO> trend;

    @Schema(description = "热销排行")
    private List<StatsHotDishVO> hotDishes;

    @Schema(description = "区间券 ROI")
    private List<StatsCouponRoiVO> couponRoi;

    @Schema(description = "会员复购、留存与高价值用户")
    private StatsMemberVO member;
}
