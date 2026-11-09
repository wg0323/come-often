package com.comeoften.module.merchant.service;

import com.comeoften.module.merchant.vo.SoldOutCategoryVO;

import java.util.List;

/**
 * 商家端菜品售罄。走 M2/M3 的 DishStockService，DB 仍是事实源，
 * 顾客端下单校验直接读 DB，因此售罄后立即不可下单。
 */
public interface MerchantSoldOutService {

    /**
     * 按分类分组的在售菜品与当日库存。
     */
    List<SoldOutCategoryVO> list(Long storeId);

    /**
     * 一键售罄 / 开售。开售时按未配置 50 份的口径补齐总量。
     */
    void toggle(Long storeId, Long dishId, boolean soldOut);
}
