package com.comeoften.module.reservation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约时段容量桶。capacity 是该桌型当前可用桌数，由 store_table 聚合；
 * booked 是 DB 兜底字段，与 Redis 的 slot:cap:{slotId} 保持同口径。
 */
@Data
@TableName("reservation_slot")
public class ReservationSlot implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long storeId;
    private LocalDate reserveDate;
    private String timeSlot;
    private String tableType;
    private Integer capacity;
    private Integer booked;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
