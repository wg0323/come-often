package com.comeoften.module.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 桌台。桌号与容量全部由数据配置，代码不写死任何桌号。
 */
@Data
@TableName("store_table")
public class StoreTable implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long storeId;
    private String tableNo;
    private String tableType;
    private Integer capacity;
    private Integer status;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
