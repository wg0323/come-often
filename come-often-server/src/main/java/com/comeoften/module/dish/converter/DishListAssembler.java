package com.comeoften.module.dish.converter;

import com.comeoften.module.dish.entity.Dish;
import com.comeoften.module.dish.entity.DishCategory;
import com.comeoften.module.dish.entity.DishImage;
import com.comeoften.module.dish.entity.DishStock;
import com.comeoften.module.dish.entity.DishTag;
import com.comeoften.module.dish.entity.DishTagRel;
import com.comeoften.module.dish.mapper.DishCategoryMapper;
import com.comeoften.module.dish.mapper.DishImageMapper;
import com.comeoften.module.dish.mapper.DishStockMapper;
import com.comeoften.module.dish.mapper.DishTagMapper;
import com.comeoften.module.dish.mapper.DishTagRelMapper;
import com.comeoften.module.dish.vo.DishListVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 菜品列表项批量装配。分类名、列表图、标签与当日库存一次性批量查回，避免 N+1。
 * 顾客端列表、后台分页与首页招牌菜共用同一份口径。
 */
@Component
@RequiredArgsConstructor
public class DishListAssembler {

    private static final String IMAGE_TYPE_LIST = "LIST";

    private final DishCategoryMapper dishCategoryMapper;

    private final DishImageMapper dishImageMapper;

    private final DishTagRelMapper dishTagRelMapper;

    private final DishTagMapper dishTagMapper;

    private final DishStockMapper dishStockMapper;

    public List<DishListVO> assemble(Long storeId, List<Dish> dishes) {
        if (dishes == null || dishes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> dishIds = dishes.stream().map(Dish::getId).collect(Collectors.toList());
        Map<Long, String> categoryNames = loadCategoryNames(storeId);
        Map<Long, String> listImages = loadListImages(dishIds);
        Map<Long, List<String>> tags = loadTags(dishIds);
        Map<Long, Integer> remains = loadRemains(storeId, dishIds);
        return dishes.stream()
                .map(dish -> {
                    Integer remain = remains.get(dish.getId());
                    boolean soldOut = remain != null && remain <= 0;
                    return DishConverter.toListVO(dish,
                            categoryNames.getOrDefault(dish.getCategoryId(), ""),
                            listImages.get(dish.getId()),
                            tags.getOrDefault(dish.getId(), Collections.emptyList()),
                            soldOut);
                })
                .collect(Collectors.toList());
    }

    private Map<Long, String> loadCategoryNames(Long storeId) {
        return dishCategoryMapper.selectList(new LambdaQueryWrapper<DishCategory>()
                        .eq(DishCategory::getStoreId, storeId))
                .stream()
                .collect(Collectors.toMap(DishCategory::getId,
                        category -> DishConverter.displayCategoryName(category.getCategoryName()),
                        (left, right) -> left));
    }

    private Map<Long, String> loadListImages(List<Long> dishIds) {
        return dishImageMapper.selectList(new LambdaQueryWrapper<DishImage>()
                        .in(DishImage::getDishId, dishIds)
                        .eq(DishImage::getImageType, IMAGE_TYPE_LIST)
                        .orderByAsc(DishImage::getSort))
                .stream()
                .collect(Collectors.toMap(DishImage::getDishId, DishImage::getObjectKey,
                        (left, right) -> left));
    }

    private Map<Long, List<String>> loadTags(List<Long> dishIds) {
        List<DishTagRel> rels = dishTagRelMapper.selectList(new LambdaQueryWrapper<DishTagRel>()
                .in(DishTagRel::getDishId, dishIds));
        if (rels.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> tagIds = rels.stream().map(DishTagRel::getTagId).distinct().collect(Collectors.toList());
        Map<Long, String> names = dishTagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(DishTag::getId, DishTag::getTagName, (left, right) -> left));
        Map<Long, List<String>> result = new HashMap<>();
        for (DishTagRel rel : rels) {
            String name = names.get(rel.getTagId());
            if (name != null) {
                result.computeIfAbsent(rel.getDishId(), key -> new ArrayList<>()).add(name);
            }
        }
        return result;
    }

    private Map<Long, Integer> loadRemains(Long storeId, List<Long> dishIds) {
        return dishStockMapper.selectList(new LambdaQueryWrapper<DishStock>()
                        .eq(DishStock::getStoreId, storeId)
                        .eq(DishStock::getStockDate, LocalDate.now())
                        .in(DishStock::getDishId, dishIds))
                .stream()
                .collect(Collectors.toMap(DishStock::getDishId, DishStock::getRemain,
                        (left, right) -> left, HashMap::new));
    }
}
