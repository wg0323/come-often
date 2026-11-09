package com.comeoften.module.review.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评价图。DB 只存 objectKey（review/ 前缀），不存完整 URL。
 */
@Data
@TableName("review_image")
public class ReviewImage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long reviewId;
    private String objectKey;
    private Integer sort;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
