package com.comeoften.module.review.service;

import com.comeoften.common.PageResult;
import com.comeoften.module.review.dto.ReviewCreateDTO;
import com.comeoften.module.review.vo.DishRatingVO;
import com.comeoften.module.review.vo.ReviewVO;

import java.util.Collection;
import java.util.Set;

/**
 * 订单评价。一单一评（review.uk_order_member），可选关联一道菜；评价成功按
 * comeoften.points.review-points 发放积分，复用积分账户的业务幂等键（biz_id = reviewId）。
 */
public interface ReviewService {

    /**
     * 提交评价。订单必须是本人本店的已完成订单，重复提交返回 30051。
     */
    ReviewVO create(Long memberId, Long storeId, ReviewCreateDTO dto);

    /**
     * 菜品评价分页，按下评价时间倒序。
     */
    PageResult<ReviewVO> pageByDish(Long dishId, Long current, Long size);

    /**
     * 菜品评分聚合，无评价时平均分 0.0、条数 0。
     */
    DishRatingVO ratingOf(Long dishId);

    /**
     * 会员已评价过的订单 ID 集合，供订单列表 / 详情计算 canReview。
     */
    Set<Long> reviewedOrderIds(Long memberId, Collection<Long> orderIds);
}
