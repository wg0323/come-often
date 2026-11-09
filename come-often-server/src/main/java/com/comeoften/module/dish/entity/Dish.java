package com.comeoften.module.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品。金额一律 BigDecimal，与 DB decimal(10,2) 对应。
 */
@Data
@TableName("dish")
public class Dish implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long storeId;
    private Long categoryId;
    private String dishName;
    private String subtitle;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer monthlySales;
    private String allergen;
    private String craftInfo;
    private Integer isPreorder;
    private Integer preorderHours;
    private Integer status;
    private Integer sort;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
