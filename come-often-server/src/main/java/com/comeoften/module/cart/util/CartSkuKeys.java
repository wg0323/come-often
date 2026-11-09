package com.comeoften.module.cart.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 购物车 SKU 组合键。规格 / 加料 ID 排序去重后拼接，保证相同组合只占一行，
 * 且与 cart_item.uk_cart_sku 的唯一键语义一致。
 */
public final class CartSkuKeys {

    private static final String SEPARATOR = ",";

    private CartSkuKeys() {
    }

    public static String build(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        Set<Long> distinct = new LinkedHashSet<>(ids);
        List<Long> sorted = new ArrayList<>(distinct);
        Collections.sort(sorted);
        StringBuilder builder = new StringBuilder();
        for (Long id : sorted) {
            if (builder.length() > 0) {
                builder.append(SEPARATOR);
            }
            builder.append(id);
        }
        return builder.toString();
    }

    /**
     * 还原组合键为 ID 列表，供金额试算与下单时复用 M2 的规格 / 加料校验。
     */
    public static List<Long> parse(String key) {
        if (key == null || key.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : key.split(SEPARATOR)) {
            ids.add(Long.valueOf(part));
        }
        return ids;
    }
}
