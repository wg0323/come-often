package com.comeoften.module.stats.converter;

import com.comeoften.common.MaskUtils;
import com.comeoften.module.coupon.constant.CouponType;
import com.comeoften.module.stats.excel.CouponRoiExportRow;
import com.comeoften.module.stats.excel.HighValueMemberExportRow;
import com.comeoften.module.stats.excel.HotDishExportRow;
import com.comeoften.module.stats.excel.TrendExportRow;
import com.comeoften.module.stats.util.StatsAmountUtils;
import com.comeoften.module.stats.vo.StatsCouponRoiRow;
import com.comeoften.module.stats.vo.StatsCouponRoiVO;
import com.comeoften.module.stats.vo.StatsHighValueMemberRow;
import com.comeoften.module.stats.vo.StatsHighValueMemberVO;
import com.comeoften.module.stats.vo.StatsHotDishRow;
import com.comeoften.module.stats.vo.StatsHotDishVO;
import com.comeoften.module.stats.vo.StatsSummaryVO;
import com.comeoften.module.stats.vo.StatsTrendPointVO;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 报表聚合行 → VO。存在非等价映射（排名、脱敏、净额、比率），统一收在这里，
 * 页面与服务都不重复算。
 */
public final class StatsConverter {

    private static final DateTimeFormatter EXPORT_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private StatsConverter() {
    }

    public static StatsSummaryVO toSummary(BigDecimal paidAmount, BigDecimal refundAmount, long orderCount) {
        StatsSummaryVO summary = new StatsSummaryVO();
        BigDecimal netRevenue = StatsAmountUtils.net(paidAmount, refundAmount);
        summary.setNetRevenue(netRevenue);
        summary.setRefundAmount(StatsAmountUtils.scale(refundAmount));
        summary.setOrderCount(orderCount);
        summary.setAvgOrderAmount(StatsAmountUtils.average(netRevenue, orderCount));
        return summary;
    }

    public static List<StatsHotDishVO> toHotDishVOs(List<StatsHotDishRow> rows) {
        List<StatsHotDishVO> result = new ArrayList<>(rows == null ? 0 : rows.size());
        if (rows == null) {
            return result;
        }
        int rank = 1;
        for (StatsHotDishRow row : rows) {
            StatsHotDishVO vo = new StatsHotDishVO();
            vo.setRank(rank++);
            vo.setDishId(row.getDishId());
            vo.setDishName(row.getDishName() == null ? "" : row.getDishName());
            vo.setQuantity(row.getQuantity() == null ? 0L : row.getQuantity());
            vo.setAmount(StatsAmountUtils.scale(row.getAmount()));
            vo.setOrderCount(row.getOrderCount() == null ? 0L : row.getOrderCount());
            result.add(vo);
        }
        return result;
    }

    public static List<StatsCouponRoiVO> toCouponRoiVOs(List<StatsCouponRoiRow> rows) {
        List<StatsCouponRoiVO> result = new ArrayList<>(rows == null ? 0 : rows.size());
        if (rows == null) {
            return result;
        }
        for (StatsCouponRoiRow row : rows) {
            StatsCouponRoiVO vo = new StatsCouponRoiVO();
            vo.setTemplateId(row.getTemplateId());
            vo.setTemplateName(row.getTemplateName());
            vo.setCouponType(row.getCouponType());
            vo.setCouponTypeText(CouponType.describe(row.getCouponType()));
            vo.setTotal(row.getTotal() == null ? 0 : row.getTotal());
            long received = row.getReceivedCount() == null ? 0L : row.getReceivedCount();
            long used = row.getUsedCount() == null ? 0L : row.getUsedCount();
            vo.setReceivedCount(received);
            vo.setUsedCount(used);
            vo.setUseRate(StatsAmountUtils.percent(used, received));
            vo.setOrderCount(row.getOrderCount() == null ? 0L : row.getOrderCount());
            BigDecimal revenue = StatsAmountUtils.net(row.getGrossPaid(), row.getRefundAmount());
            BigDecimal cost = StatsAmountUtils.zeroIfNull(row.getDiscountAmount());
            vo.setRevenueAmount(revenue);
            vo.setDiscountAmount(StatsAmountUtils.scale(cost));
            vo.setRoi(StatsAmountUtils.ratio(revenue, cost));
            result.add(vo);
        }
        return result;
    }

    public static List<TrendExportRow> toTrendExportRows(List<StatsTrendPointVO> points) {
        List<TrendExportRow> rows = new ArrayList<>(points == null ? 0 : points.size());
        if (points == null) {
            return rows;
        }
        for (StatsTrendPointVO point : points) {
            TrendExportRow row = new TrendExportRow();
            row.setBucketStart(point.getBucketStart());
            row.setNetRevenue(point.getNetRevenue());
            row.setRefundAmount(point.getRefundAmount());
            row.setOrderCount(point.getOrderCount());
            row.setAvgOrderAmount(point.getAvgOrderAmount());
            rows.add(row);
        }
        return rows;
    }

    public static List<HotDishExportRow> toHotDishExportRows(List<StatsHotDishVO> dishes) {
        List<HotDishExportRow> rows = new ArrayList<>(dishes == null ? 0 : dishes.size());
        if (dishes == null) {
            return rows;
        }
        for (StatsHotDishVO dish : dishes) {
            HotDishExportRow row = new HotDishExportRow();
            row.setRank(dish.getRank());
            row.setDishName(dish.getDishName());
            row.setQuantity(dish.getQuantity());
            row.setAmount(dish.getAmount());
            row.setOrderCount(dish.getOrderCount());
            rows.add(row);
        }
        return rows;
    }

    public static List<CouponRoiExportRow> toCouponRoiExportRows(List<StatsCouponRoiVO> items) {
        List<CouponRoiExportRow> rows = new ArrayList<>(items == null ? 0 : items.size());
        if (items == null) {
            return rows;
        }
        for (StatsCouponRoiVO item : items) {
            CouponRoiExportRow row = new CouponRoiExportRow();
            row.setTemplateName(item.getTemplateName());
            row.setCouponTypeText(item.getCouponTypeText());
            row.setTotal(item.getTotal());
            row.setReceivedCount(item.getReceivedCount());
            row.setUsedCount(item.getUsedCount());
            row.setUseRate(item.getUseRate());
            row.setOrderCount(item.getOrderCount());
            row.setRevenueAmount(item.getRevenueAmount());
            row.setDiscountAmount(item.getDiscountAmount());
            row.setRoi(item.getRoi());
            rows.add(row);
        }
        return rows;
    }

    public static List<HighValueMemberExportRow> toHighValueMemberExportRows(List<StatsHighValueMemberVO> members) {
        List<HighValueMemberExportRow> rows = new ArrayList<>(members == null ? 0 : members.size());
        if (members == null) {
            return rows;
        }
        for (StatsHighValueMemberVO member : members) {
            HighValueMemberExportRow row = new HighValueMemberExportRow();
            row.setRank(member.getRank());
            row.setNickname(member.getNickname());
            row.setPhoneMasked(member.getPhoneMasked());
            row.setLevelName(member.getLevelName());
            row.setOrderCount(member.getOrderCount());
            row.setNetAmount(member.getNetAmount());
            row.setLastPaidTime(member.getLastPaidTime() == null
                    ? "" : member.getLastPaidTime().format(EXPORT_TIMESTAMP_FORMATTER));
            rows.add(row);
        }
        return rows;
    }

    public static List<StatsHighValueMemberVO> toHighValueMemberVOs(List<StatsHighValueMemberRow> rows) {
        List<StatsHighValueMemberVO> result = new ArrayList<>(rows == null ? 0 : rows.size());
        if (rows == null) {
            return result;
        }
        int rank = 1;
        for (StatsHighValueMemberRow row : rows) {
            StatsHighValueMemberVO vo = new StatsHighValueMemberVO();
            vo.setRank(rank++);
            vo.setMemberId(row.getMemberId());
            vo.setNickname(row.getNickname() == null ? "" : row.getNickname());
            String maskedPhone = MaskUtils.maskPhone(row.getPhone());
            vo.setPhoneMasked(maskedPhone == null ? "" : maskedPhone);
            vo.setLevelName(row.getLevelName() == null ? "" : row.getLevelName());
            vo.setOrderCount(row.getOrderCount() == null ? 0L : row.getOrderCount());
            vo.setNetAmount(StatsAmountUtils.net(row.getPaidAmount(), row.getRefundAmount()));
            vo.setLastPaidTime(row.getLastPaidTime());
            result.add(vo);
        }
        return result;
    }
}
