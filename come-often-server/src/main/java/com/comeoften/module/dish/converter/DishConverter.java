package com.comeoften.module.dish.converter;

import com.comeoften.module.dish.entity.Dish;
import com.comeoften.module.dish.entity.DishAddon;
import com.comeoften.module.dish.entity.DishCategory;
import com.comeoften.module.dish.entity.DishImage;
import com.comeoften.module.dish.entity.DishSpec;
import com.comeoften.module.dish.entity.DishSpecOption;
import com.comeoften.module.dish.util.DishConstants;
import com.comeoften.module.dish.util.PresaleStatusResolver;
import com.comeoften.module.dish.vo.DishCategoryVO;
import com.comeoften.module.dish.vo.DishDetailVO;
import com.comeoften.module.dish.vo.DishListVO;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 菜品对象转换。预定菜的展示名映射、角标与定金说明集中在此，避免散落在各 Service。
 */
public final class DishConverter {

    private DishConverter() {
    }

    /**
     * 组装列表项。同名同类型字段一行拷贝，其余为非等价映射单独赋值。
     */
    public static DishListVO toListVO(Dish dish, String categoryName, String listImage,
                                      List<String> tags, boolean soldOut) {
        DishListVO vo = new DishListVO();
        BeanUtils.copyProperties(dish, vo);
        vo.setCategoryName(categoryName);
        vo.setListImage(listImage);
        vo.setTags(tags);
        vo.setSoldOut(soldOut);
        vo.setPreorder(isPreorder(dish));
        String presaleStatus = presaleStatusOf(dish);
        vo.setPresale(dish.getPresaleStartTime() != null);
        vo.setPresaleStatus(presaleStatus);
        vo.setPresaleText(PresaleStatusResolver.text(presaleStatus, dish.getPresaleStartTime()));
        return vo;
    }

    /**
     * 组装详情基础字段；图片、规格、加料、标签等聚合列表由调用方补齐。
     */
    public static DishDetailVO toDetailBase(Dish dish, String categoryName) {
        DishDetailVO vo = new DishDetailVO();
        BeanUtils.copyProperties(dish, vo);
        vo.setCategoryName(categoryName);
        boolean preorder = isPreorder(dish);
        vo.setPreorder(preorder);
        vo.setPreorderHours(preorder ? DishConstants.PREORDER_HOURS : 0);
        vo.setPreorderBadge(preorder ? DishConstants.PREORDER_BADGE : "");
        vo.setPreorderNote(preorder ? DishConstants.PREORDER_NOTE : "");
        vo.setDepositAmount(preorder ? DishConstants.PREORDER_DEPOSIT : BigDecimal.ZERO);
        vo.setOnlyDineIn(preorder);
        String presaleStatus = presaleStatusOf(dish);
        vo.setPresale(dish.getPresaleStartTime() != null);
        vo.setPresaleStatus(presaleStatus);
        vo.setPresaleText(PresaleStatusResolver.text(presaleStatus, dish.getPresaleStartTime()));
        return vo;
    }

    private static String presaleStatusOf(Dish dish) {
        return PresaleStatusResolver.resolve(dish.getPresaleStartTime(),
                dish.getPresaleEndTime(), LocalDateTime.now());
    }

    /**
     * 分类 VO。顾客端把「预定」显示为「需提前预定」。
     */
    public static DishCategoryVO toCategoryVO(DishCategory category, long dishCount, boolean forApp) {
        DishCategoryVO vo = new DishCategoryVO();
        vo.setId(category.getId());
        vo.setStorageName(category.getCategoryName());
        boolean preorder = DishConstants.PREORDER_CATEGORY_STORAGE.equals(category.getCategoryName());
        vo.setPreorder(preorder);
        vo.setCategoryName(forApp && preorder
                ? DishConstants.PREORDER_CATEGORY_DISPLAY : category.getCategoryName());
        vo.setSort(category.getSort());
        vo.setDishCount(dishCount);
        return vo;
    }

    public static String displayCategoryName(String storageName) {
        return DishConstants.PREORDER_CATEGORY_STORAGE.equals(storageName)
                ? DishConstants.PREORDER_CATEGORY_DISPLAY : storageName;
    }

    public static DishDetailVO.ImageVO toImageVO(DishImage image) {
        DishDetailVO.ImageVO vo = new DishDetailVO.ImageVO();
        BeanUtils.copyProperties(image, vo);
        return vo;
    }

    public static DishDetailVO.SpecOptionVO toSpecOptionVO(DishSpecOption option) {
        DishDetailVO.SpecOptionVO vo = new DishDetailVO.SpecOptionVO();
        BeanUtils.copyProperties(option, vo);
        return vo;
    }

    public static DishDetailVO.AddonVO toAddonVO(DishAddon addon) {
        DishDetailVO.AddonVO vo = new DishDetailVO.AddonVO();
        BeanUtils.copyProperties(addon, vo);
        return vo;
    }

    public static DishDetailVO.SpecGroupVO toSpecGroupVO(DishSpec spec, List<DishSpecOption> options) {
        DishDetailVO.SpecGroupVO vo = new DishDetailVO.SpecGroupVO();
        BeanUtils.copyProperties(spec, vo);
        vo.setRequired(Objects.equals(spec.getIsRequired(), DishConstants.YES));
        vo.setOptions(options.stream().map(DishConverter::toSpecOptionVO).collect(Collectors.toList()));
        return vo;
    }

    public static boolean isPreorder(Dish dish) {
        return Objects.equals(dish.getIsPreorder(), DishConstants.YES);
    }

    public static boolean isOnShelf(Dish dish) {
        return Objects.equals(dish.getStatus(), DishConstants.STATUS_ON_SHELF);
    }
}
