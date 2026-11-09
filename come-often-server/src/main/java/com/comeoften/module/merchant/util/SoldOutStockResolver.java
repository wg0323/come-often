package com.comeoften.module.merchant.util;

import com.comeoften.module.dish.util.DishConstants;

/**
 * 商家端售罄 / 开售的总量口径。
 *
 * <p>售罄只把余量置 0，总量保持不动；开售必须带回一个正的总量，否则
 * <code>DishStockService.apply</code> 会按「未配置 → total = 0」落库，页面点了开售仍然是售罄。
 * 未配置过当日库存的菜品沿用菜品域的默认备货（{@link DishConstants#DEFAULT_STOCK_TOTAL}）。</p>
 */
public final class SoldOutStockResolver {

    private SoldOutStockResolver() {
    }

    /**
     * 计算提交给库存服务的总量。售罄返回 null，表示沿用当前总量。
     */
    public static Integer resolveTotal(Boolean configured, Integer currentTotal, boolean soldOut) {
        if (soldOut) {
            return null;
        }
        if (configured == null || !configured) {
            return DishConstants.DEFAULT_STOCK_TOTAL;
        }
        if (currentTotal == null || currentTotal <= 0) {
            return DishConstants.DEFAULT_STOCK_TOTAL;
        }
        return currentTotal;
    }
}
