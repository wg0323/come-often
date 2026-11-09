package com.comeoften.module.store.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 门店营业时间，按星期配置。
 */
@Data
@TableName("store_business_hour")
public class StoreBusinessHour implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long storeId;
    private Integer weekDay;
    private LocalTime openTime;
    private LocalTime closeTime;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
