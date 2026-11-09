package com.comeoften.module.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.module.dish.dto.DishStockDTO;
import com.comeoften.module.dish.entity.Dish;
import com.comeoften.module.dish.entity.DishCategory;
import com.comeoften.module.dish.mapper.DishCategoryMapper;
import com.comeoften.module.dish.mapper.DishMapper;
import com.comeoften.module.dish.service.DishStockService;
import com.comeoften.module.dish.util.DishConstants;
import com.comeoften.module.dish.vo.DishStockVO;
import com.comeoften.module.merchant.service.MerchantSoldOutService;
import com.comeoften.module.merchant.util.SoldOutStockResolver;
import com.comeoften.module.merchant.vo.SoldOutCategoryVO;
import com.comeoften.module.merchant.vo.SoldOutDishVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 商家端售罄实现。库存读写全部委托 DishStockService，不另写一套库存规则。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantSoldOutServiceImpl implements MerchantSoldOutService {

    private final DishCategoryMapper dishCategoryMapper;

    private final DishMapper dishMapper;

    private final DishStockService dishStockService;

    @Override
    public List<SoldOutCategoryVO> list(Long storeId) {
        List<DishStockVO> stocks = dishStockService.list(storeId);
        if (stocks.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, DishStockVO> stockByDish = stocks.stream()
                .collect(Collectors.toMap(DishStockVO::getDishId, Function.identity(), (left, right) -> left));
        List<Dish> dishes = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStoreId, storeId)
                .eq(Dish::getStatus, DishConstants.STATUS_ON_SHELF)
                .orderByAsc(Dish::getSort));
        Map<Long, List<SoldOutDishVO>> grouped = new HashMap<>(8);
        for (Dish dish : dishes) {
            DishStockVO stock = stockByDish.get(dish.getId());
            if (stock == null) {
                continue;
            }
            grouped.computeIfAbsent(dish.getCategoryId(), key -> new ArrayList<>())
                    .add(toDishVO(dish, stock));
        }
        List<DishCategory> categories = dishCategoryMapper.selectList(new LambdaQueryWrapper<DishCategory>()
                .eq(DishCategory::getStoreId, storeId)
                .orderByAsc(DishCategory::getSort));
        List<SoldOutCategoryVO> result = new ArrayList<>();
        for (DishCategory category : categories) {
            List<SoldOutDishVO> items = grouped.get(category.getId());
            if (items == null || items.isEmpty()) {
                continue;
            }
            SoldOutCategoryVO vo = new SoldOutCategoryVO();
            vo.setCategoryId(category.getId());
            vo.setCategoryName(category.getCategoryName());
            vo.setDishes(items);
            result.add(vo);
        }
        return result;
    }

    @Override
    public void toggle(Long storeId, Long dishId, boolean soldOut) {
        DishStockVO current = dishStockService.get(storeId, dishId);
        DishStockDTO dto = new DishStockDTO();
        dto.setDishId(dishId);
        dto.setSoldOut(soldOut);
        dto.setTotal(SoldOutStockResolver.resolveTotal(current.getConfigured(), current.getTotal(), soldOut));
        dishStockService.apply(storeId, dto);
        log.info("商家端售罄操作完成: storeId={}, dishId={}, soldOut={}", storeId, dishId, soldOut);
    }

    private SoldOutDishVO toDishVO(Dish dish, DishStockVO stock) {
        SoldOutDishVO vo = new SoldOutDishVO();
        vo.setDishId(dish.getId());
        vo.setDishName(dish.getDishName());
        vo.setPrice(dish.getPrice());
        vo.setTotal(stock.getTotal());
        vo.setRemain(stock.getRemain());
        vo.setSoldOut(stock.getSoldOut());
        vo.setConfigured(stock.getConfigured());
        return vo;
    }
}
