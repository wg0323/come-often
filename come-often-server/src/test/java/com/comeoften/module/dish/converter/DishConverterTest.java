package com.comeoften.module.dish.converter;

import com.comeoften.module.dish.entity.Dish;
import com.comeoften.module.dish.entity.DishCategory;
import com.comeoften.module.dish.util.DishConstants;
import com.comeoften.module.dish.vo.DishCategoryVO;
import com.comeoften.module.dish.vo.DishDetailVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 预定菜展示规则与上下架判定的纯逻辑测试。
 */
class DishConverterTest {

    @Test
    void should_fill_badge_and_deposit_when_dish_is_preorder() {
        Dish dish = buildDish();
        dish.setIsPreorder(DishConstants.YES);
        DishDetailVO vo = DishConverter.toDetailBase(dish, DishConstants.PREORDER_CATEGORY_DISPLAY);
        assertThat(vo.getPreorder()).isTrue();
        assertThat(vo.getPreorderBadge()).isEqualTo("需提前 1 天预定");
        assertThat(vo.getPreorderNote()).contains("200 元定金").contains("定金不退").contains("仅限到店用餐");
        assertThat(vo.getDepositAmount()).isEqualByComparingTo("200.00");
        assertThat(vo.getPreorderHours()).isEqualTo(DishConstants.PREORDER_HOURS);
        assertThat(vo.getOnlyDineIn()).isTrue();
    }

    @Test
    void should_hide_preorder_marks_when_dish_is_normal() {
        Dish dish = buildDish();
        DishDetailVO vo = DishConverter.toDetailBase(dish, "家常");
        assertThat(vo.getPreorder()).isFalse();
        assertThat(vo.getPreorderBadge()).isEmpty();
        assertThat(vo.getDepositAmount()).isEqualByComparingTo("0");
        assertThat(vo.getOnlyDineIn()).isFalse();
    }

    @Test
    void should_map_preorder_category_to_customer_display_name() {
        DishCategory category = new DishCategory();
        category.setId(103L);
        category.setCategoryName(DishConstants.PREORDER_CATEGORY_STORAGE);
        category.setSort(3);
        DishCategoryVO app = DishConverter.toCategoryVO(category, 3L, true);
        DishCategoryVO admin = DishConverter.toCategoryVO(category, 3L, false);
        assertThat(app.getCategoryName()).isEqualTo("需提前预定");
        assertThat(app.getStorageName()).isEqualTo("预定");
        assertThat(app.getPreorder()).isTrue();
        assertThat(admin.getCategoryName()).isEqualTo("预定");
    }

    @Test
    void should_tell_on_shelf_only_when_status_is_one() {
        Dish dish = buildDish();
        dish.setStatus(DishConstants.STATUS_OFF_SHELF);
        assertThat(DishConverter.isOnShelf(dish)).isFalse();
        dish.setStatus(DishConstants.STATUS_ON_SHELF);
        assertThat(DishConverter.isOnShelf(dish)).isTrue();
    }

    private Dish buildDish() {
        Dish dish = new Dish();
        dish.setId(1003L);
        dish.setStoreId(1L);
        dish.setCategoryId(103L);
        dish.setDishName("油爆双脆");
        dish.setPrice(new BigDecimal("238.00"));
        dish.setStatus(DishConstants.STATUS_ON_SHELF);
        dish.setIsPreorder(DishConstants.NO);
        return dish;
    }
}
