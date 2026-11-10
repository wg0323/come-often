package com.comeoften.module.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.review.entity.Review;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 评价表。评分聚合用一条聚合 SQL 完成，条数与平均分同源，避免两次查询不自洽。
 */
public interface ReviewMapper extends BaseMapper<Review> {

    /**
     * 菜品评分聚合；无评价时 reviewCount 为 0、averageRating 为 NULL。
     */
    @Select("SELECT COUNT(*) AS review_count, AVG(rating) AS average_rating "
            + "FROM review WHERE dish_id = #{dishId} AND is_deleted = 0 AND status = 'APPROVED'")
    ReviewRatingAggregate ratingOf(@Param("dishId") Long dishId);

    /**
     * 审核状态推进：带旧状态条件的 CAS，防并发重复审核。
     */
    @Update("UPDATE review SET status = #{targetStatus}, audit_user_id = #{operatorId}, "
            + "audit_time = NOW(), update_time = NOW() "
            + "WHERE id = #{id} AND store_id = #{storeId} AND is_deleted = 0 "
            + "AND status <> #{targetStatus}")
    int audit(@Param("id") Long id, @Param("storeId") Long storeId,
              @Param("targetStatus") String targetStatus, @Param("operatorId") Long operatorId);
}
