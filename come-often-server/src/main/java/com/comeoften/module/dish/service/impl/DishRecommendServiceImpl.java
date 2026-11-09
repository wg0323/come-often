package com.comeoften.module.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comeoften.module.dish.converter.DishListAssembler;
import com.comeoften.module.dish.entity.Dish;
import com.comeoften.module.dish.entity.DishStock;
import com.comeoften.module.dish.mapper.DishMapper;
import com.comeoften.module.dish.mapper.DishStockMapper;
import com.comeoften.module.dish.service.DishRecommendService;
import com.comeoften.module.dish.util.DishConstants;
import com.comeoften.module.dish.vo.DishListVO;
import com.comeoften.module.order.constant.OrderStatus;
import com.comeoften.module.order.entity.OrderItem;
import com.comeoften.module.order.entity.TradeOrder;
import com.comeoften.module.order.mapper.OrderItemMapper;
import com.comeoften.module.order.mapper.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 历史订单推荐实现。
 *
 * <p>只取本店 COMPLETED 订单（退款 / 取消的订单不进推荐），按完成时间倒序取 3 单，
 * 菜品按「最近一单 → 更早一单」去重；下架与今日售罄的菜直接剔除，避免点进去不能下单。</p>
 */
@Service
@RequiredArgsConstructor
public class DishRecommendServiceImpl implements DishRecommendService {

    private static final long RECENT_ORDER_LIMIT = 3L;

    private final TradeOrderMapper tradeOrderMapper;

    private final OrderItemMapper orderItemMapper;

    private final DishMapper dishMapper;

    private final DishStockMapper dishStockMapper;

    private final DishListAssembler dishListAssembler;

    @Override
    public List<DishListVO> listFromHistory(Long memberId, Long storeId) {
        if (memberId == null || storeId == null) {
            return Collections.emptyList();
        }
        // count=false：推荐只关心这 3 单，不需要总数
        IPage<TradeOrder> page = tradeOrderMapper.selectPage(new Page<>(1L, RECENT_ORDER_LIMIT, false),
                new LambdaQueryWrapper<TradeOrder>()
                        .eq(TradeOrder::getMemberId, memberId)
                        .eq(TradeOrder::getStoreId, storeId)
                        .eq(TradeOrder::getStatus, OrderStatus.COMPLETED.name())
                        .orderByDesc(TradeOrder::getCompletedTime)
                        .orderByDesc(TradeOrder::getId));
        List<TradeOrder> orders = page.getRecords();
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, List<OrderItem>> itemsByOrder = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>()
                                .in(OrderItem::getOrderId,
                                        orders.stream().map(TradeOrder::getId).collect(Collectors.toList())))
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
        // LinkedHashSet 保留「最近一单优先」的顺序，同时完成跨订单去重
        Set<Long> dishIds = new LinkedHashSet<>();
        for (TradeOrder order : orders) {
            for (OrderItem item : itemsByOrder.getOrDefault(order.getId(), Collections.emptyList())) {
                if (item.getDishId() != null) {
                    dishIds.add(item.getDishId());
                }
            }
        }
        if (dishIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Dish> dishes = dishMapper.selectBatchIds(dishIds);
        if (dishes.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Dish> dishMap = dishes.stream()
                .collect(Collectors.toMap(Dish::getId, dish -> dish, (left, right) -> left));
        Map<Long, Integer> remains = loadTodayRemains(storeId, dishIds);
        List<Dish> available = new ArrayList<>();
        for (Long dishId : dishIds) {
            Dish dish = dishMap.get(dishId);
            if (dish == null || !Objects.equals(storeId, dish.getStoreId())
                    || !Integer.valueOf(DishConstants.STATUS_ON_SHELF).equals(dish.getStatus())) {
                continue;
            }
            Integer remain = remains.get(dishId);
            if (remain != null && remain <= 0) {
                continue;
            }
            available.add(dish);
        }
        return dishListAssembler.assemble(storeId, available);
    }

    private Map<Long, Integer> loadTodayRemains(Long storeId, Set<Long> dishIds) {
        return dishStockMapper.selectList(new LambdaQueryWrapper<DishStock>()
                        .eq(DishStock::getStoreId, storeId)
                        .eq(DishStock::getStockDate, LocalDate.now())
                        .in(DishStock::getDishId, dishIds))
                .stream()
                .collect(Collectors.toMap(DishStock::getDishId, DishStock::getRemain,
                        (left, right) -> left, HashMap::new));
    }
}
