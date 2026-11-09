package com.comeoften.module.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜品与标签关联。
 */
@Data
@TableName("dish_tag_rel")
public class DishTagRel implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long dishId;
    private Long tagId;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
