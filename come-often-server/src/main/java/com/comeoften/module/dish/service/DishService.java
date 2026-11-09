package com.comeoften.module.dish.service;

import com.comeoften.common.PageResult;
import com.comeoften.module.dish.dto.DishSaveDTO;
import com.comeoften.module.dish.vo.DishDetailVO;
import com.comeoften.module.dish.vo.DishListVO;

/**
 * 后台菜品管理：CRUD、上下架与排序。规格、加料、图片、标签随菜品整体保存。
 * 所有操作显式带 storeId，避免跨门店越权。
 */
public interface DishService {

    PageResult<DishListVO> page(Long storeId, Long current, Long size, String keyword,
                                Long categoryId, Integer status);

    DishDetailVO detailForAdmin(Long storeId, Long dishId);

    Long save(Long storeId, DishSaveDTO dto);

    void remove(Long storeId, Long dishId);

    void updateStatus(Long storeId, Long dishId, Integer status);

    void updateSort(Long storeId, Long dishId, Integer sort);
}
