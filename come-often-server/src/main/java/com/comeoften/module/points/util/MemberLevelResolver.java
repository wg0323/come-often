package com.comeoften.module.points.util;

import com.comeoften.module.points.entity.MemberLevel;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * 会员等级判定。门槛全部来自 member_level 数据，代码不写死任何档位；
 * 年度消费额与累计积分必须同时达标，避免「只攒积分不消费」也能升级。
 */
public final class MemberLevelResolver {

    private MemberLevelResolver() {
    }

    /**
     * 取同时满足两项门槛的最高等级；都不满足（或配置为空）时返回 null。
     */
    public static MemberLevel highest(Collection<MemberLevel> levels, BigDecimal annualAmount,
                                      Integer totalEarned) {
        if (levels == null || levels.isEmpty()) {
            return null;
        }
        BigDecimal amount = annualAmount == null ? BigDecimal.ZERO : annualAmount;
        int points = totalEarned == null ? 0 : totalEarned;
        MemberLevel best = null;
        for (MemberLevel level : levels) {
            if (level == null || level.getMinAmount() == null || level.getMinPoints() == null) {
                continue;
            }
            if (amount.compareTo(level.getMinAmount()) < 0 || points < level.getMinPoints()) {
                continue;
            }
            if (best == null || sortOf(level) > sortOf(best)) {
                best = level;
            }
        }
        return best;
    }

    /**
     * 等级排序值，null 视为 0，避免配置漏填时 NPE。
     */
    public static int sortOf(MemberLevel level) {
        return level == null || level.getSort() == null ? 0 : level.getSort();
    }
}
