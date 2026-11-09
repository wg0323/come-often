package com.comeoften.module.cart.service;

import com.comeoften.module.cart.dto.CartAddDTO;
import com.comeoften.module.cart.vo.CartVO;

/**
 * 购物车。金额一律由服务端按 /dish/quote 口径试算，页面不自行计算。
 */
public interface CartService {

    CartVO list(Long memberId, Long storeId);

    String add(Long memberId, Long storeId, CartAddDTO dto);

    void updateQuantity(Long memberId, Long storeId, Long itemId, Integer quantity);

    void remove(Long memberId, Long storeId, Long itemId);

    void clear(Long memberId, Long storeId);
}
