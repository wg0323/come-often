package com.comeoften.module.review.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单评价。一单一评由 uk_order_member(order_id, member_id) 兜底，
 * dish_id 可空，最多关联一道菜，不做评价与菜品的多对多关联。
 */
@Data
@TableName("review")
public class Review implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long storeId;
    private Long orderId;
    private Long memberId;
    private Long dishId;
    private Integer rating;
    private String content;
    private Integer isWithImage;
    /** 先发后审，DB 默认 APPROVED；B4 起评分聚合只统计通过的评价。 */
    private String status;
    private Long auditUserId;
    private LocalDateTime auditTime;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
