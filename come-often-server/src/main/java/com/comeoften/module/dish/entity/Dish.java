package com.comeoften.module.dish.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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
    private BigDecimal memberPrice;
    /** DB 列名 is_member_only，POJO 布尔属性按规范去掉 is 前缀。 */
    @TableField("is_member_only")
    private Integer memberOnly;
    private LocalDateTime presaleStartTime;
    private LocalDateTime presaleEndTime;
    private Integer presaleStock;
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
