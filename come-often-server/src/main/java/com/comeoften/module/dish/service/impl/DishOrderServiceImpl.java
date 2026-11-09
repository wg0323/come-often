package com.comeoften.module.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.dish.converter.DishConverter;
import com.comeoften.module.dish.dto.DishQuoteDTO;
import com.comeoften.module.dish.dto.OrderCheckDTO;
import com.comeoften.module.dish.entity.Dish;
import com.comeoften.module.dish.entity.DishAddon;
import com.comeoften.module.dish.entity.DishSpec;
import com.comeoften.module.dish.entity.DishSpecOption;
import com.comeoften.module.dish.entity.DishStock;
import com.comeoften.module.dish.mapper.DishAddonMapper;
import com.comeoften.module.dish.mapper.DishMapper;
import com.comeoften.module.dish.mapper.DishSpecMapper;
import com.comeoften.module.dish.mapper.DishSpecOptionMapper;
import com.comeoften.module.dish.mapper.DishStockMapper;
import com.comeoften.module.dish.service.DishOrderService;
import com.comeoften.module.dish.util.DishConstants;
import com.comeoften.module.dish.util.DishPriceCalculator;
import com.comeoften.module.dish.vo.DishQuoteVO;
import com.comeoften.module.dish.vo.OrderCheckVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 金额试算与下单前校验。规格/加料的归属校验、必选校验与金额汇总集中在此，M3 下单直接复用。
 */
@Service
@RequiredArgsConstructor
public class DishOrderServiceImpl implements DishOrderService {

    private static final int SCALE = 2;

    private final DishMapper dishMapper;

    private final DishSpecMapper dishSpecMapper;

    private final DishSpecOptionMapper dishSpecOptionMapper;

    private final DishAddonMapper dishAddonMapper;

    private final DishStockMapper dishStockMapper;

    @Override
    public DishQuoteVO quote(DishQuoteDTO dto) {
        Dish dish = requireDish(dto.getDishId(), dto.getStoreId());
        if (!DishConverter.isOnShelf(dish)) {
            throw new BizException(ErrorCode.DISH_OFF_SHELF);
        }
        int quantity = normalizeQuantity(dto.getQuantity());
        Additions additions = resolveAdditions(dish, dto.getSpecOptionIds(), dto.getAddonIds());
        BigDecimal unitPrice = DishPriceCalculator.quote(dish.getPrice(),
                additions.specDeltas, additions.addonDeltas, 1);
        DishQuoteVO vo = new DishQuoteVO();
        vo.setDishId(dish.getId());
        vo.setDishName(dish.getDishName());
        vo.setBasePrice(dish.getPrice());
        vo.setSpecAmount(additions.specAmount);
        vo.setAddonAmount(additions.addonAmount);
        vo.setUnitPrice(unitPrice);
        vo.setQuantity(quantity);
        vo.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(SCALE, RoundingMode.HALF_UP));
        boolean preorder = DishConverter.isPreorder(dish);
        vo.setPreorder(preorder);
        vo.setDepositAmount(preorder
                ? DishConstants.PREORDER_DEPOSIT.min(vo.getTotalPrice()).setScale(SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        return vo;
    }

    @Override
    public OrderCheckVO check(OrderCheckDTO dto) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        boolean preorder = false;
        List<OrderCheckVO.ItemVO> rows = new ArrayList<>();
        // 同一菜品拆成多行时先汇总份数，避免逐行判定绕过库存
        Map<Long, Integer> quantityByDish = new HashMap<>();
        for (OrderCheckDTO.ItemDTO item : dto.getItems()) {
            quantityByDish.merge(item.getDishId(), normalizeQuantity(item.getQuantity()), Integer::sum);
        }
        quantityByDish.forEach((dishId, total) -> assertOrderable(dto.getStoreId(), dishId, total));
        for (OrderCheckDTO.ItemDTO item : dto.getItems()) {
            int quantity = normalizeQuantity(item.getQuantity());
            Dish dish = requireDish(item.getDishId(), dto.getStoreId());
            Additions additions = resolveAdditions(dish, item.getSpecOptionIds(), item.getAddonIds());
            BigDecimal unitPrice = DishPriceCalculator.quote(dish.getPrice(),
                    additions.specDeltas, additions.addonDeltas, 1);
            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(quantity))
                    .setScale(SCALE, RoundingMode.HALF_UP);
            OrderCheckVO.ItemVO row = new OrderCheckVO.ItemVO();
            row.setDishId(dish.getId());
            row.setDishName(dish.getDishName());
            row.setQuantity(quantity);
            row.setUnitPrice(unitPrice);
            row.setAmount(amount);
            rows.add(row);
            totalAmount = totalAmount.add(amount);
            preorder = preorder || DishConverter.isPreorder(dish);
        }
        OrderCheckVO vo = new OrderCheckVO();
        vo.setOrderable(Boolean.TRUE);
        vo.setTotalAmount(totalAmount.setScale(SCALE, RoundingMode.HALF_UP));
        vo.setPreorder(preorder);
        vo.setDepositAmount(preorder
                ? totalAmount.min(DishConstants.PREORDER_DEPOSIT).setScale(SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        vo.setItems(rows);
        return vo;
    }

    @Override
    public void assertOrderable(Long storeId, Long dishId, int quantity) {
        Dish dish = requireDish(dishId, storeId);
        if (!DishConverter.isOnShelf(dish)) {
            throw new BizException(ErrorCode.DISH_OFF_SHELF);
        }
        DishStock stock = dishStockMapper.selectOne(new LambdaQueryWrapper<DishStock>()
                .eq(DishStock::getStoreId, storeId)
                .eq(DishStock::getDishId, dishId)
                .eq(DishStock::getStockDate, LocalDate.now()));
        if (stock != null && stock.getRemain() < quantity) {
            throw new BizException(ErrorCode.DISH_SOLD_OUT);
        }
    }

    /**
     * 校验已选规格与加料确实属于该菜品，并汇总加价。必选规格未选、同组多选均拒绝。
     */
    private Additions resolveAdditions(Dish dish, List<Long> specOptionIds, List<Long> addonIds) {
        List<DishSpec> specs = dishSpecMapper.selectList(new LambdaQueryWrapper<DishSpec>()
                .eq(DishSpec::getDishId, dish.getId())
                .orderByAsc(DishSpec::getSort));
        List<Long> specIds = specs.stream().map(DishSpec::getId).collect(Collectors.toList());
        Map<Long, DishSpecOption> optionById = specIds.isEmpty() ? Collections.emptyMap()
                : dishSpecOptionMapper.selectList(new LambdaQueryWrapper<DishSpecOption>()
                        .in(DishSpecOption::getSpecId, specIds))
                .stream().collect(Collectors.toMap(DishSpecOption::getId,
                        Function.identity(), (left, right) -> left));
        List<Long> selectedOptionIds = distinct(specOptionIds);
        List<BigDecimal> specDeltas = new ArrayList<>();
        for (Long optionId : selectedOptionIds) {
            DishSpecOption option = optionById.get(optionId);
            if (option == null) {
                throw new BizException(ErrorCode.DISH_SPEC_INVALID, "规格选项不属于该菜品");
            }
            specDeltas.add(option.getPriceDelta());
        }
        for (DishSpec spec : specs) {
            long count = selectedOptionIds.stream()
                    .filter(optionId -> spec.getId().equals(optionById.get(optionId).getSpecId()))
                    .count();
            if (count > 1) {
                throw new BizException(ErrorCode.DISH_SPEC_INVALID, "规格「" + spec.getSpecName() + "」只能选一项");
            }
            if (count == 0 && Objects.equals(spec.getIsRequired(), DishConstants.YES)) {
                throw new BizException(ErrorCode.DISH_SPEC_INVALID, "请选择「" + spec.getSpecName() + "」");
            }
        }
        Map<Long, DishAddon> addonById = dishAddonMapper.selectList(new LambdaQueryWrapper<DishAddon>()
                        .eq(DishAddon::getDishId, dish.getId()))
                .stream().collect(Collectors.toMap(DishAddon::getId,
                        Function.identity(), (left, right) -> left));
        List<BigDecimal> addonDeltas = new ArrayList<>();
        for (Long addonId : distinct(addonIds)) {
            DishAddon addon = addonById.get(addonId);
            if (addon == null) {
                throw new BizException(ErrorCode.DISH_ADDON_INVALID, "加料不属于该菜品");
            }
            addonDeltas.add(addon.getPriceDelta());
        }
        return new Additions(specDeltas, addonDeltas, sum(specDeltas), sum(addonDeltas));
    }

    private Dish requireDish(Long dishId, Long storeId) {
        if (storeId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "门店不能为空");
        }
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null || !storeId.equals(dish.getStoreId())) {
            throw new BizException(ErrorCode.DISH_NOT_FOUND);
        }
        return dish;
    }

    private int normalizeQuantity(Integer quantity) {
        int value = quantity == null ? 1 : quantity;
        if (value <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "菜品份数必须大于 0");
        }
        return value;
    }

    private static List<Long> distinct(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> set = new LinkedHashSet<>(ids);
        return new ArrayList<>(set);
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        BigDecimal result = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            result = result.add(value == null ? BigDecimal.ZERO : value);
        }
        return result.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 已选项的加价明细，纯度数据载体，不外泄到接口层。
     */
    private static final class Additions {

        private final List<BigDecimal> specDeltas;

        private final List<BigDecimal> addonDeltas;

        private final BigDecimal specAmount;

        private final BigDecimal addonAmount;

        private Additions(List<BigDecimal> specDeltas, List<BigDecimal> addonDeltas,
                          BigDecimal specAmount, BigDecimal addonAmount) {
            this.specDeltas = specDeltas;
            this.addonDeltas = addonDeltas;
            this.specAmount = specAmount;
            this.addonAmount = addonAmount;
        }
    }
}
