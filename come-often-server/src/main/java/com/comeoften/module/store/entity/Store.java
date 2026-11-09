package com.comeoften.module.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 门店。
 */
@Data
@TableName("store")
public class Store implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String storeName;
    private String address;
    private String phone;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer businessStatus;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
