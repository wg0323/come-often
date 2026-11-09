package com.comeoften.module.dish.service;

import com.comeoften.module.dish.dto.DishQuoteDTO;
import com.comeoften.module.dish.dto.OrderCheckDTO;
import com.comeoften.module.dish.vo.DishQuoteVO;
import com.comeoften.module.dish.vo.OrderCheckVO;

/**
 * 金额试算与下单前校验。M3 下单主流程复用 assertOrderable，业务规则只有一份。
 */
public interface DishOrderService {

    DishQuoteVO quote(DishQuoteDTO dto);

    OrderCheckVO check(OrderCheckDTO dto);

    /**
     * 断言菜品可下单：必须上架且当日库存充足。不满足抛 BizException。
     */
    void assertOrderable(Long storeId, Long dishId, int quantity);
}
