package com.comeoften.module.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.cart.dto.CartAddDTO;
import com.comeoften.module.cart.entity.CartItem;
import com.comeoften.module.cart.mapper.CartItemMapper;
import com.comeoften.module.cart.service.CartService;
import com.comeoften.module.cart.util.CartSkuKeys;
import com.comeoften.module.cart.vo.CartVO;
import com.comeoften.module.dish.converter.DishConverter;
import com.comeoften.module.dish.dto.DishQuoteDTO;
import com.comeoften.module.dish.entity.Dish;
import com.comeoften.module.dish.entity.DishAddon;
import com.comeoften.module.dish.entity.DishImage;
import com.comeoften.module.dish.entity.DishSpecOption;
import com.comeoften.module.dish.mapper.DishAddonMapper;
import com.comeoften.module.dish.mapper.DishImageMapper;
import com.comeoften.module.dish.mapper.DishMapper;
import com.comeoften.module.dish.mapper.DishSpecOptionMapper;
import com.comeoften.module.dish.service.DishOrderService;
import com.comeoften.module.dish.vo.DishQuoteVO;
import com.comeoften.module.order.util.OrderAmountCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购物车实现。加购前复用 M2 的 DishOrderService.quote / assertOrderable，
 * 保证「规格加料合法性 + 上架 + 当日库存」与下单主流程是同一份规则。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final String IMAGE_TYPE_LIST = "LIST";

    private static final String IMAGE_TYPE_THUMB = "THUMB";

    private final CartItemMapper cartItemMapper;

    private final DishMapper dishMapper;

    private final DishImageMapper dishImageMapper;

    private final DishSpecOptionMapper dishSpecOptionMapper;

    private final DishAddonMapper dishAddonMapper;

    private final DishOrderService dishOrderService;

    @Override
    public CartVO list(Long memberId, Long storeId) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getMemberId, memberId)
                .eq(CartItem::getStoreId, storeId)
                .orderByAsc(CartItem::getCreateTime));
        Map<Long, Dish> dishMap = loadDishes(items);
        Map<Long, String> imageMap = loadImages(items);
        List<CartVO.ItemVO> rows = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        boolean containsPreorder = false;
        for (CartItem item : items) {
            CartVO.ItemVO row = toRow(item, dishMap.get(item.getDishId()), imageMap, storeId);
            rows.add(row);
            if (!Boolean.TRUE.equals(row.getInvalid())) {
                totalAmount = totalAmount.add(row.getAmount());
                totalQuantity += row.getQuantity();
                containsPreorder = containsPreorder || Boolean.TRUE.equals(row.getPreorder());
            }
        }
        CartVO vo = new CartVO();
        vo.setItems(rows);
        vo.setTotalAmount(OrderAmountCalculator.scale(totalAmount));
        vo.setTotalQuantity(totalQuantity);
        vo.setContainsPreorder(containsPreorder);
        vo.setDepositAmount(OrderAmountCalculator.deposit(totalAmount, containsPreorder));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String add(Long memberId, Long storeId, CartAddDTO dto) {
        int quantity = normalizeQuantity(dto.getQuantity());
        List<Long> specIds = distinct(dto.getSpecOptionIds());
        List<Long> addonIds = distinct(dto.getAddonIds());
        DishQuoteDTO quoteDTO = new DishQuoteDTO();
        quoteDTO.setDishId(dto.getDishId());
        quoteDTO.setQuantity(quantity);
        quoteDTO.setSpecOptionIds(specIds);
        quoteDTO.setAddonIds(addonIds);
        quoteDTO.setStoreId(storeId);
        dishOrderService.quote(quoteDTO);
        dishOrderService.assertOrderable(storeId, dto.getDishId(), quantity);
        String specKey = CartSkuKeys.build(specIds);
        String addonKeys = CartSkuKeys.build(addonIds);
        CartItem existing = cartItemMapper.selectRawBySku(memberId, storeId, dto.getDishId(),
                specKey, addonKeys);
        if (existing == null) {
            CartItem fresh = new CartItem();
            fresh.setMemberId(memberId);
            fresh.setStoreId(storeId);
            fresh.setDishId(dto.getDishId());
            fresh.setSpecKey(specKey);
            fresh.setAddonKeys(addonKeys);
            fresh.setQuantity(quantity);
            try {
                cartItemMapper.insert(fresh);
            } catch (DuplicateKeyException e) {
                cartItemMapper.restoreExisting(memberId, storeId, dto.getDishId(),
                        specKey, addonKeys, quantity);
            }
            return String.valueOf(requireId(memberId, storeId, dto.getDishId(), specKey, addonKeys));
        }
        if (Objects.equals(existing.getIsDeleted(), 1)) {
            cartItemMapper.restoreExisting(memberId, storeId, dto.getDishId(),
                    specKey, addonKeys, quantity);
        } else {
            cartItemMapper.incrementExisting(memberId, storeId, dto.getDishId(),
                    specKey, addonKeys, quantity);
        }
        return String.valueOf(existing.getId());
    }

    @Override
    public void updateQuantity(Long memberId, Long storeId, Long itemId, Integer quantity) {
        CartItem item = requireOwned(memberId, storeId, itemId);
        item.setQuantity(normalizeQuantity(quantity));
        cartItemMapper.updateById(item);
    }

    @Override
    public void remove(Long memberId, Long storeId, Long itemId) {
        requireOwned(memberId, storeId, itemId);
        cartItemMapper.deleteById(itemId);
    }

    @Override
    public void clear(Long memberId, Long storeId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getMemberId, memberId)
                .eq(CartItem::getStoreId, storeId));
    }

    private CartVO.ItemVO toRow(CartItem item, Dish dish, Map<Long, String> imageMap, Long storeId) {
        List<Long> specIds = CartSkuKeys.parse(item.getSpecKey());
        List<Long> addonIds = CartSkuKeys.parse(item.getAddonKeys());
        CartVO.ItemVO row = new CartVO.ItemVO();
        row.setItemId(item.getId());
        row.setDishId(item.getDishId());
        row.setDishName(dish == null ? "菜品已下架" : dish.getDishName());
        row.setObjectKey(imageMap.getOrDefault(item.getDishId(), ""));
        row.setSpecOptionIds(specIds);
        row.setAddonIds(addonIds);
        row.setSpecDesc(describeOptions(specIds));
        row.setAddonDesc(describeAddons(addonIds));
        row.setQuantity(item.getQuantity());
        row.setPreorder(dish != null && DishConverter.isPreorder(dish));
        row.setInvalid(Boolean.FALSE);
        row.setInvalidReason("");
        row.setUnitPrice(BigDecimal.ZERO);
        row.setAmount(BigDecimal.ZERO);
        if (dish == null) {
            row.setInvalid(Boolean.TRUE);
            row.setInvalidReason("菜品已下架或不属于当前门店");
            return row;
        }
        try {
            DishQuoteDTO quoteDTO = new DishQuoteDTO();
            quoteDTO.setDishId(item.getDishId());
            quoteDTO.setQuantity(item.getQuantity());
            quoteDTO.setSpecOptionIds(specIds);
            quoteDTO.setAddonIds(addonIds);
            quoteDTO.setStoreId(storeId);
            DishQuoteVO quote = dishOrderService.quote(quoteDTO);
            dishOrderService.assertOrderable(storeId, item.getDishId(), item.getQuantity());
            row.setUnitPrice(quote.getUnitPrice());
            row.setAmount(quote.getTotalPrice());
        } catch (BizException e) {
            row.setInvalid(Boolean.TRUE);
            row.setInvalidReason(e.getMessage());
        }
        return row;
    }

    private Map<Long, Dish> loadDishes(List<CartItem> items) {
        List<Long> ids = items.stream().map(CartItem::getDishId).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return dishMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Dish::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, String> loadImages(List<CartItem> items) {
        List<Long> ids = items.stream().map(CartItem::getDishId).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<DishImage> images = dishImageMapper.selectList(new LambdaQueryWrapper<DishImage>()
                .in(DishImage::getDishId, ids));
        Map<Long, String> result = new java.util.HashMap<>();
        for (DishImage image : images) {
            if (IMAGE_TYPE_LIST.equals(image.getImageType()) && !result.containsKey(image.getDishId())) {
                result.put(image.getDishId(), image.getObjectKey());
            }
        }
        for (DishImage image : images) {
            if (IMAGE_TYPE_THUMB.equals(image.getImageType())) {
                result.putIfAbsent(image.getDishId(), image.getObjectKey());
            }
        }
        return result;
    }

    private String describeOptions(List<Long> ids) {
        if (ids.isEmpty()) {
            return "";
        }
        return dishSpecOptionMapper.selectBatchIds(ids).stream()
                .map(DishSpecOption::getOptionName)
                .collect(Collectors.joining("、"));
    }

    private String describeAddons(List<Long> ids) {
        if (ids.isEmpty()) {
            return "";
        }
        return dishAddonMapper.selectBatchIds(ids).stream()
                .map(DishAddon::getAddonName)
                .collect(Collectors.joining("、"));
    }

    private CartItem requireOwned(Long memberId, Long storeId, Long itemId) {
        CartItem item = cartItemMapper.selectById(itemId);
        if (item == null || !memberId.equals(item.getMemberId()) || !storeId.equals(item.getStoreId())) {
            throw new BizException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        return item;
    }

    private Long requireId(Long memberId, Long storeId, Long dishId, String specKey, String addonKeys) {
        CartItem item = cartItemMapper.selectRawBySku(memberId, storeId, dishId, specKey, addonKeys);
        if (item == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "购物车行写入失败");
        }
        return item.getId();
    }

    private static int normalizeQuantity(Integer quantity) {
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
}
