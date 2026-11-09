package com.comeoften.module.dish.service;

import com.comeoften.module.dish.dto.CategorySaveDTO;
import com.comeoften.module.dish.vo.DishCategoryVO;

import java.util.List;

/**
 * 菜品分类管理。「预定」在顾客端映射为「需提前预定」，映射只发生在 VO 层。
 */
public interface DishCategoryService {

    List<DishCategoryVO> listForApp(Long storeId);

    List<DishCategoryVO> listForAdmin(Long storeId);

    Long save(Long storeId, CategorySaveDTO dto);

    void remove(Long storeId, Long categoryId);

    void updateSort(Long storeId, Long categoryId, Integer sort);
}
