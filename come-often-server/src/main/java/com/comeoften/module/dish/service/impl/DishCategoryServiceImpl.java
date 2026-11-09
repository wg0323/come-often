package com.comeoften.module.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.dish.converter.DishConverter;
import com.comeoften.module.dish.dto.CategorySaveDTO;
import com.comeoften.module.dish.entity.Dish;
import com.comeoften.module.dish.entity.DishCategory;
import com.comeoften.module.dish.mapper.DishCategoryMapper;
import com.comeoften.module.dish.mapper.DishMapper;
import com.comeoften.module.dish.service.DishCategoryService;
import com.comeoften.module.dish.util.DishConstants;
import com.comeoften.module.dish.vo.DishCategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 分类管理。顾客端只展示 is_show=1 的分类，且把「预定」显示为「需提前预定」。
 */
@Service
@RequiredArgsConstructor
public class DishCategoryServiceImpl implements DishCategoryService {

    private final DishCategoryMapper dishCategoryMapper;

    private final DishMapper dishMapper;

    @Override
    public List<DishCategoryVO> listForApp(Long storeId) {
        return list(storeId, true);
    }

    @Override
    public List<DishCategoryVO> listForAdmin(Long storeId) {
        return list(storeId, false);
    }

    @Override
    public Long save(Long storeId, CategorySaveDTO dto) {
        if (dto.getId() == null) {
            assertNameAvailable(storeId, dto.getCategoryName(), null);
            DishCategory category = new DishCategory();
            category.setStoreId(storeId);
            category.setCategoryName(dto.getCategoryName());
            category.setSort(dto.getSort() == null ? 0 : dto.getSort());
            category.setIsShow(Boolean.FALSE.equals(dto.getShow()) ? DishConstants.NO : DishConstants.YES);
            dishCategoryMapper.insert(category);
            return category.getId();
        }
        require(storeId, dto.getId());
        assertNameAvailable(storeId, dto.getCategoryName(), dto.getId());
        DishCategory patch = new DishCategory();
        patch.setId(dto.getId());
        patch.setCategoryName(dto.getCategoryName());
        patch.setSort(dto.getSort());
        if (dto.getShow() != null) {
            patch.setIsShow(dto.getShow() ? DishConstants.YES : DishConstants.NO);
        }
        dishCategoryMapper.updateById(patch);
        return dto.getId();
    }

    @Override
    public void remove(Long storeId, Long categoryId) {
        require(storeId, categoryId);
        long count = dishMapper.selectCount(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStoreId, storeId)
                .eq(Dish::getCategoryId, categoryId));
        if (count > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "分类下仍有菜品，不能删除");
        }
        dishCategoryMapper.deleteById(categoryId);
    }

    @Override
    public void updateSort(Long storeId, Long categoryId, Integer sort) {
        require(storeId, categoryId);
        DishCategory patch = new DishCategory();
        patch.setId(categoryId);
        patch.setSort(sort == null ? 0 : sort);
        dishCategoryMapper.updateById(patch);
    }

    private List<DishCategoryVO> list(Long storeId, boolean forApp) {
        List<DishCategory> categories = dishCategoryMapper.selectList(new LambdaQueryWrapper<DishCategory>()
                .eq(DishCategory::getStoreId, storeId)
                .eq(forApp, DishCategory::getIsShow, DishConstants.YES)
                .orderByAsc(DishCategory::getSort)
                .orderByAsc(DishCategory::getId));
        if (categories.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Long> counts = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                        .select(Dish::getId, Dish::getCategoryId)
                        .eq(Dish::getStoreId, storeId)
                        .eq(Dish::getStatus, DishConstants.STATUS_ON_SHELF))
                .stream().collect(Collectors.groupingBy(Dish::getCategoryId, Collectors.counting()));
        return categories.stream()
                .map(category -> DishConverter.toCategoryVO(category,
                        counts.getOrDefault(category.getId(), 0L), forApp))
                .collect(Collectors.toList());
    }

    private void assertNameAvailable(Long storeId, String name, Long excludeId) {
        long count = dishCategoryMapper.selectCount(new LambdaQueryWrapper<DishCategory>()
                .eq(DishCategory::getStoreId, storeId)
                .eq(DishCategory::getCategoryName, name)
                .ne(excludeId != null, DishCategory::getId, excludeId));
        if (count > 0) {
            throw new BizException(ErrorCode.DISH_CATEGORY_NAME_DUPLICATE);
        }
    }

    private void require(Long storeId, Long categoryId) {
        DishCategory category = dishCategoryMapper.selectById(categoryId);
        if (category == null || !Objects.equals(category.getStoreId(), storeId)) {
            throw new BizException(ErrorCode.DISH_CATEGORY_NOT_FOUND);
        }
    }
}
