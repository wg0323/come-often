package com.comeoften.module.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜品标签定义。
 */
@Data
@TableName("dish_tag")
public class DishTag implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String tagName;
    private String tagCode;
    private String tagIcon;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
