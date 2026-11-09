package com.comeoften.module.home.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 首页轮播。
 */
@Data
@TableName("banner")
public class Banner implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long storeId;
    private String title;
    private String objectKey;
    private String jumpType;
    private String jumpValue;
    private Integer weight;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
