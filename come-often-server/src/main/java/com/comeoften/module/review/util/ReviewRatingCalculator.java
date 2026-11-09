package com.comeoften.module.review.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * 评分聚合口径：平均分保留一位小数、四舍五入；无评分返回 0.0，页面不用判空。
 */
public final class ReviewRatingCalculator {

    private static final int SCALE = 1;

    private ReviewRatingCalculator() {
    }

    /**
     * 平均分。入参可能含空值，空值按缺省处理而不是当 0 分。
     */
    public static BigDecimal average(Collection<Integer> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        long sum = 0L;
        long count = 0L;
        for (Integer rating : ratings) {
            if (rating == null) {
                continue;
            }
            sum += rating;
            count++;
        }
        if (count == 0L) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(count), SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 单条聚合 SQL 返回的平均分再做一次精度收敛。
     */
    public static BigDecimal average(BigDecimal rawAverage) {
        if (rawAverage == null) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return rawAverage.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
