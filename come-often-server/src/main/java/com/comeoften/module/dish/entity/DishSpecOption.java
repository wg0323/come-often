package com.comeoften.module.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 规格选项，priceDelta 为相对基础价的加价。
 */
@Data
@TableName("dish_spec_option")
public class DishSpecOption implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long specId;
    private String optionName;
    private BigDecimal priceDelta;
    private Integer sort;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
