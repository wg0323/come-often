package com.comeoften.module.review.mapper;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 菜品评分聚合的查询结果。条数与平均分来自同一条 SQL，避免两次查询之间插入新评价
 * 造成「条数 5 条、平均分按 6 条算」的自相矛盾。
 */
@Data
public class ReviewRatingAggregate implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long reviewCount;

    private BigDecimal averageRating;
}
