package com.comeoften.module.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜品图。只存 objectKey，不存完整 URL。
 */
@Data
@TableName("dish_image")
public class DishImage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long dishId;
    private String objectKey;
    private String imageType;
    private Integer sort;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
