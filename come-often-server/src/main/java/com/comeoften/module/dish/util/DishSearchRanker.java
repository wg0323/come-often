package com.comeoften.module.dish.util;

import com.comeoften.module.dish.entity.Dish;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * 搜索排序口径：菜名前缀命中 &gt; 标签命中 &gt; 副标题命中 &gt; 描述命中，
 * 同一档内招牌优先，再按运营排序值与 ID 升序，保证分页结果稳定可复现。
 *
 * <p>排序放在 Java 里做，SQL 只负责用右模糊把候选集捞出来——db-spec 禁止左模糊与全模糊，
 * 同时避免把用户输入拼进 ORDER BY。此实现属于 DishSearchService 的内部细节，
 * 后续换 Elasticsearch 时只替换实现，接口与分页契约不变。</p>
 */
public final class DishSearchRanker {

    /**
     * 菜名前缀命中优先级。
     */
    public static final int RANK_NAME = 1;

    /**
     * 标签命中优先级。
     */
    public static final int RANK_TAG = 2;

    /**
     * 副标题命中优先级。
     */
    public static final int RANK_SUBTITLE = 3;

    /**
     * 描述命中优先级。
     */
    public static final int RANK_DESCRIPTION = 4;

    /**
     * 兜底优先级，正常不会出现（候选集只由前四类条件产生）。
     */
    public static final int RANK_OTHER = 5;

    private DishSearchRanker() {
    }

    /**
     * 单条匹配优先级，数值越小越靠前。
     */
    public static int rankOf(String keyword, Dish dish, Collection<String> tagNames) {
        String keywordNormalized = normalize(keyword);
        if (keywordNormalized.isEmpty() || dish == null) {
            return RANK_OTHER;
        }
        if (startsWith(dish.getDishName(), keywordNormalized)) {
            return RANK_NAME;
        }
        if (tagNames != null && tagNames.stream().anyMatch(tag -> startsWith(tag, keywordNormalized))) {
            return RANK_TAG;
        }
        if (startsWith(dish.getSubtitle(), keywordNormalized)) {
            return RANK_SUBTITLE;
        }
        if (startsWith(dish.getDescription(), keywordNormalized)) {
            return RANK_DESCRIPTION;
        }
        return RANK_OTHER;
    }

    /**
     * 排序并返回新列表，不改调用方的入参顺序。
     *
     * @param tagLookup        按菜品 ID 取标签名，可为 null
     * @param signatureDishIds 本店招牌菜 ID 集合，可为 null
     */
    public static List<Dish> sort(String keyword, List<Dish> candidates,
                                  Function<Long, Collection<String>> tagLookup,
                                  Set<Long> signatureDishIds) {
        boolean hasSignature = signatureDishIds != null && !signatureDishIds.isEmpty();
        Comparator<Dish> comparator = Comparator
                .comparingInt((Dish dish) -> rankOf(keyword, dish,
                        tagLookup == null ? null : tagLookup.apply(dish.getId())))
                .thenComparingInt(dish -> hasSignature && signatureDishIds.contains(dish.getId()) ? 0 : 1)
                .thenComparingInt(dish -> dish.getSort() == null ? Integer.MAX_VALUE : dish.getSort())
                .thenComparing(Dish::getId);
        List<Dish> sorted = new ArrayList<>(candidates);
        sorted.sort(comparator);
        return sorted;
    }

    /**
     * 关键词归一：去首尾空白并转小写，兼容英文与拼音输入法。
     */
    public static String normalize(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean startsWith(String value, String normalizedKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).startsWith(normalizedKeyword);
    }
}
