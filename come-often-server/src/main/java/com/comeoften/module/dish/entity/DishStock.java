package com.comeoften.module.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 菜品每日库存。DB 的 remain 为最终事实源，Redis 只作快速判定层。
 */
@Data
@TableName("dish_stock")
public class DishStock implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long storeId;
    private Long dishId;
    private LocalDate stockDate;
    private Integer total;
    private Integer remain;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
