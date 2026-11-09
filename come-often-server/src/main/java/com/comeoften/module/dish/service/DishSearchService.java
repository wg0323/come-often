package com.comeoften.module.dish.service;

import com.comeoften.common.PageResult;
import com.comeoften.module.dish.dto.DishSearchDTO;
import com.comeoften.module.dish.vo.DishListVO;

/**
 * 菜品搜索。Controller 与页面只依赖本接口与统一的 DishListVO：
 * 当前实现是「菜名 / 副标题 / 描述右模糊 + 标签关联」，后续换 Elasticsearch
 * 只替换实现，不动接口契约与分页语义。
 */
public interface DishSearchService {

    /**
     * 关键词分页搜索。关键词为空时返回空分页，不返回全量菜品。
     */
    PageResult<DishListVO> search(DishSearchDTO query);
}
