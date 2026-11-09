package com.comeoften.module.dish.util;

import java.math.BigDecimal;

/**
 * 菜品域固定业务口径。预定菜规则见开发计划 4.7，定金按整单 200 元、整单不足 200 时按整单收。
 */
public final class DishConstants {

    public static final BigDecimal PREORDER_DEPOSIT = new BigDecimal("200.00");

    public static final int PREORDER_HOURS = 24;

    public static final String PREORDER_BADGE = "需提前 1 天预定";

    public static final String PREORDER_NOTE = "付 200 元定金锁定，定金不退，仅限到店用餐";

    public static final String PREORDER_CATEGORY_STORAGE = "预定";

    public static final String PREORDER_CATEGORY_DISPLAY = "需提前预定";

    public static final String SIGNATURE_TAG_CODE = "SIGNATURE";

    /** 新建菜品与商家端「开售」未配置库存时的默认备货数，唯一真值。 */
    public static final int DEFAULT_STOCK_TOTAL = 50;

    public static final int STATUS_OFF_SHELF = 0;

    public static final int STATUS_ON_SHELF = 1;

    public static final int YES = 1;

    public static final int NO = 0;

    private DishConstants() {
    }
}
