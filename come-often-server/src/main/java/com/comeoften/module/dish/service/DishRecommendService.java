package com.comeoften.module.dish.service;

import com.comeoften.module.dish.vo.DishListVO;

import java.util.List;

/**
 * 基于历史订单的菜品推荐。只服务登录会员：取本店最近 3 笔已完成订单的菜品，
 * 去重并排除下架与今日售罄，无历史时返回空列表（页面据此隐藏模块）。
 */
public interface DishRecommendService {

    /**
     * 最近 3 单吃过的菜，按最近一单的时间倒序。
     */
    List<DishListVO> listFromHistory(Long memberId, Long storeId);
}
