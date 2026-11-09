package com.comeoften.module.stats.service.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.stats.constant.StatsGranularity;
import com.comeoften.module.stats.converter.StatsConverter;
import com.comeoften.module.stats.mapper.StatsMapper;
import com.comeoften.module.stats.service.StatsService;
import com.comeoften.module.stats.util.StatsAmountUtils;
import com.comeoften.module.stats.util.StatsRangeResolver;
import com.comeoften.module.stats.util.StatsTrendAggregator;
import com.comeoften.module.stats.vo.StatsDailyRefundRow;
import com.comeoften.module.stats.vo.StatsDailyRevenueRow;
import com.comeoften.module.stats.vo.StatsDashboardVO;
import com.comeoften.module.stats.vo.StatsDateRange;
import com.comeoften.module.stats.vo.StatsMemberAggRow;
import com.comeoften.module.stats.vo.StatsMemberVO;
import com.comeoften.module.stats.vo.StatsRangeVO;
import com.comeoften.module.stats.vo.StatsReportVO;
import com.comeoften.module.stats.vo.StatsReservationDayVO;
import com.comeoften.module.stats.vo.StatsReservationStatusRow;
import com.comeoften.module.stats.vo.StatsRetentionRow;
import com.comeoften.module.stats.vo.StatsSummaryVO;
import com.comeoften.module.reservation.constant.ReservationStatus;
import com.comeoften.module.store.entity.Store;
import com.comeoften.module.store.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 看板与报表实现。所有数字来自 StatsMapper 的聚合 SQL，页面不做任何业务计算。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    /** 榜单默认条数。 */
    private static final int DEFAULT_TOP_N = 10;

    /** 榜单上限，防止一次拉全量菜品 / 全量会员。 */
    private static final int MAX_TOP_N = 50;

    /** 经营看板的热销只要前 5，摆太多反而看不出重点。 */
    private static final int DASHBOARD_HOT_DISH_TOP_N = 5;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StatsMapper statsMapper;

    private final StoreMapper storeMapper;

    @Override
    public StatsDashboardVO dashboard(Long storeId) {
        LocalDate today = LocalDate.now();
        StatsDateRange range = StatsRangeResolver.resolve(today, today);
        List<StatsDailyRevenueRow> paidRows = statsMapper.dailyPaid(storeId,
                range.fromInclusive(), range.toExclusive());
        List<StatsDailyRefundRow> refundRows = statsMapper.dailyRefund(storeId,
                range.fromInclusive(), range.toExclusive());

        Store store = storeMapper.selectById(storeId);
        StatsDashboardVO vo = new StatsDashboardVO();
        vo.setStoreName(store == null ? "" : store.getStoreName());
        vo.setStatDate(today.toString());
        vo.setUpdatedAt(LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        vo.setSummary(buildSummary(paidRows, refundRows));
        vo.setReservations(buildReservationDay(storeId, today));
        vo.setHotDishes(StatsConverter.toHotDishVOs(statsMapper.hotDish(storeId,
                range.fromInclusive(), range.toExclusive(), DASHBOARD_HOT_DISH_TOP_N)));
        log.debug("经营看板查询完成: storeId={}, statDate={}, orderCount={}",
                storeId, today, vo.getSummary().getOrderCount());
        return vo;
    }

    @Override
    public StatsReportVO report(Long storeId, LocalDate startDate, LocalDate endDate, String granularity,
                                Integer topN) {
        StatsGranularity resolved = StatsGranularity.of(granularity);
        if (resolved == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "时间粒度只支持 DAY / WEEK / MONTH");
        }
        int limit = normalizeTopN(topN);
        StatsDateRange range = StatsRangeResolver.resolve(startDate, endDate);
        LocalDateTime from = range.fromInclusive();
        LocalDateTime to = range.toExclusive();

        List<StatsDailyRevenueRow> paidRows = statsMapper.dailyPaid(storeId, from, to);
        List<StatsDailyRefundRow> refundRows = statsMapper.dailyRefund(storeId, from, to);

        StatsReportVO vo = new StatsReportVO();
        vo.setRange(buildRangeVO(range, resolved, limit));
        vo.setSummary(buildSummary(paidRows, refundRows));
        vo.setTrend(StatsTrendAggregator.aggregate(paidRows, refundRows, range, resolved));
        vo.setHotDishes(StatsConverter.toHotDishVOs(statsMapper.hotDish(storeId, from, to, limit)));
        vo.setCouponRoi(StatsConverter.toCouponRoiVOs(statsMapper.couponRoi(storeId, from, to)));
        vo.setMember(buildMember(storeId, from, to, limit));
        log.debug("数据报表查询完成: storeId={}, range={}~{}", storeId, range.getStartDate(),
                range.getEndDate());
        return vo;
    }

    /**
     * 汇总与趋势同源：都基于同一份按日聚合结果，避免汇总走一条 SQL、趋势走另一条时对不上账。
     */
    private StatsSummaryVO buildSummary(List<StatsDailyRevenueRow> paidRows,
                                        List<StatsDailyRefundRow> refundRows) {
        BigDecimal paid = BigDecimal.ZERO;
        long orderCount = 0L;
        for (StatsDailyRevenueRow row : paidRows) {
            paid = paid.add(StatsAmountUtils.zeroIfNull(row.getPaidAmount()));
            orderCount += row.getOrderCount() == null ? 0L : row.getOrderCount();
        }
        BigDecimal refund = BigDecimal.ZERO;
        for (StatsDailyRefundRow row : refundRows) {
            refund = refund.add(StatsAmountUtils.zeroIfNull(row.getRefundAmount()));
        }
        return StatsConverter.toSummary(paid, refund, orderCount);
    }

    private StatsMemberVO buildMember(Long storeId, LocalDateTime from, LocalDateTime to, int topN) {
        StatsMemberAggRow agg = statsMapper.memberAgg(storeId, from, to);
        StatsRetentionRow retention = statsMapper.retention(storeId, from, to);
        long paidMemberCount = agg == null ? 0L : defaultLong(agg.getPaidMemberCount());
        long repurchaseCount = agg == null ? 0L : defaultLong(agg.getRepurchaseMemberCount());
        long cohortSize = retention == null ? 0L : defaultLong(retention.getCohortSize());
        long retainedCount = retention == null ? 0L : defaultLong(retention.getRetainedCount());

        StatsMemberVO vo = new StatsMemberVO();
        vo.setPaidMemberCount(paidMemberCount);
        vo.setRepurchaseMemberCount(repurchaseCount);
        vo.setRepurchaseRate(StatsAmountUtils.percent(repurchaseCount, paidMemberCount));
        vo.setRetentionCohortSize(cohortSize);
        vo.setRetentionCount(retainedCount);
        vo.setRetentionRate(StatsAmountUtils.percent(retainedCount, cohortSize));
        vo.setHighValueUsers(StatsConverter.toHighValueMemberVOs(
                statsMapper.highValueMember(storeId, from, to, topN)));
        return vo;
    }

    private StatsReservationDayVO buildReservationDay(Long storeId, LocalDate date) {
        StatsReservationDayVO vo = new StatsReservationDayVO();
        vo.setDate(date.toString());
        vo.setPending(0L);
        vo.setConfirmed(0L);
        vo.setCheckedIn(0L);
        vo.setNoShow(0L);
        vo.setCancelled(0L);
        long total = 0L;
        for (StatsReservationStatusRow row : statsMapper.reservationCounts(storeId, date)) {
            long count = defaultLong(row.getCount());
            String status = row.getStatus();
            if (ReservationStatus.PENDING.name().equals(status)) {
                vo.setPending(count);
            } else if (ReservationStatus.CONFIRMED.name().equals(status)) {
                vo.setConfirmed(count);
            } else if (ReservationStatus.CHECKED_IN.name().equals(status)) {
                vo.setCheckedIn(count);
            } else if (ReservationStatus.NO_SHOW.name().equals(status)) {
                vo.setNoShow(count);
            } else if (ReservationStatus.CANCELLED.name().equals(status)) {
                vo.setCancelled(count);
            }
            if (!ReservationStatus.CANCELLED.name().equals(status)) {
                total += count;
            }
        }
        vo.setTotal(total);
        vo.setSlots(statsMapper.slotOccupancy(storeId, date));
        return vo;
    }

    private StatsRangeVO buildRangeVO(StatsDateRange range, StatsGranularity granularity, int topN) {
        StatsRangeVO vo = new StatsRangeVO();
        vo.setStartDate(range.getStartDate().toString());
        vo.setEndDate(range.getEndDate().toString());
        vo.setGranularity(granularity.name());
        vo.setGranularityText(StatsGranularity.describe(granularity));
        vo.setTopN(topN);
        return vo;
    }

    private int normalizeTopN(Integer topN) {
        if (topN == null) {
            return DEFAULT_TOP_N;
        }
        if (topN < 1) {
            throw new BizException(ErrorCode.PARAM_INVALID, "topN 必须大于 0");
        }
        return Math.min(topN, MAX_TOP_N);
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}
