package com.comeoften.module.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品加料。
 */
@Data
@TableName("dish_addon")
public class DishAddon implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long dishId;
    private String addonName;
    private BigDecimal priceDelta;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
