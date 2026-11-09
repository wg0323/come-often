package com.comeoften.module.dish.service;

import com.comeoften.module.dish.dto.DishQueryDTO;
import com.comeoften.module.dish.vo.DishDetailVO;
import com.comeoften.module.dish.vo.DishListVO;

import java.util.List;

/**
 * 顾客端菜品展示。列表只返回上架菜品，下架菜品对顾客端不可见。
 */
public interface DishQueryService {

    List<DishListVO> list(DishQueryDTO query);

    /**
     * 顾客端详情，带门店归属校验，防止枚举 dishId 读取其他门店数据。
     */
    DishDetailVO detail(Long dishId, Long storeId);

    /**
     * 后台编辑回显，允许查看下架菜品；顾客端禁止调用。
     */
    DishDetailVO detailForAdmin(Long storeId, Long dishId);
}
