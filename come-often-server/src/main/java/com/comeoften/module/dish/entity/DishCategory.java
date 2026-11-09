package com.comeoften.module.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜品分类。预定类后台存「预定」，顾客端展示「需提前预定」。
 */
@Data
@TableName("dish_category")
public class DishCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long storeId;
    private String categoryName;
    private Integer sort;
    private Integer isShow;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
