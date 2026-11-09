package com.comeoften.module.dish.util;

import com.comeoften.module.dish.entity.Dish;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DishSearchRankerTest {

    @Test
    void should_rank_name_hit_before_tag_subtitle_and_description() {
        Dish nameHit = dish(1L, "招牌九转大肠", "", "", 100, 10);
        Dish tagHit = dish(2L, "干炸丸子", "", "", 1, 10);
        Dish subtitleHit = dish(3L, "汤", "招牌奶汤蒲菜", "", 1, 10);
        Dish descriptionHit = dish(4L, "时蔬", "", "招牌做法，选用本地时蔬", 1, 10);
        Map<Long, List<String>> tags = new HashMap<>();
        tags.put(2L, Collections.singletonList("招牌推荐"));

        List<Dish> sorted = DishSearchRanker.sort("招牌",
                Arrays.asList(descriptionHit, subtitleHit, tagHit, nameHit),
                tags::get, Collections.emptySet());

        assertThat(sorted.stream().map(Dish::getId).collect(Collectors.toList()))
                .containsExactly(1L, 2L, 3L, 4L);
    }

    @Test
    void should_put_signature_dish_first_inside_the_same_rank() {
        Dish common = dish(1L, "爆炒腰花", "", "", 1, 10);
        Dish signature = dish(2L, "爆炒双脆", "", "", 99, 10);
        Set<Long> signatureDishIds = new HashSet<>(Collections.singletonList(2L));

        List<Dish> sorted = DishSearchRanker.sort("爆炒",
                Arrays.asList(common, signature), id -> Collections.emptyList(), signatureDishIds);

        assertThat(sorted.stream().map(Dish::getId).collect(Collectors.toList()))
                .containsExactly(2L, 1L);
    }

    @Test
    void should_fall_back_to_sort_value_then_id_for_stable_paging() {
        Dish later = dish(3L, "红烧肉", "", "", 20, 10);
        Dish first = dish(1L, "红烧排骨", "", "", 10, 10);
        Dish sameSortSmallerId = dish(2L, "红烧带鱼", "", "", 10, 10);

        List<Dish> sorted = DishSearchRanker.sort("红烧",
                Arrays.asList(later, first, sameSortSmallerId), id -> null, null);

        assertThat(sorted.stream().map(Dish::getId).collect(Collectors.toList()))
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    void should_return_other_rank_when_keyword_is_blank() {
        assertThat(DishSearchRanker.rankOf("  ", dish(1L, "红烧肉", "", "", 1, 10), null))
                .isEqualTo(DishSearchRanker.RANK_OTHER);
    }

    @Test
    void should_normalize_keyword_case_and_space() {
        assertThat(DishSearchRanker.normalize("  Cola  ")).isEqualTo("cola");
        assertThat(DishSearchRanker.normalize(null)).isEmpty();
        Dish dish = dish(1L, "Cola 可乐", "", "", 1, 10);
        assertThat(DishSearchRanker.rankOf("cola", dish, null)).isEqualTo(DishSearchRanker.RANK_NAME);
    }

    private static Dish dish(Long id, String dishName, String subtitle, String description,
                             Integer sort, Integer status) {
        Dish dish = new Dish();
        dish.setId(id);
        dish.setDishName(dishName);
        dish.setSubtitle(subtitle);
        dish.setDescription(description);
        dish.setSort(sort);
        dish.setStatus(status);
        return dish;
    }
}
