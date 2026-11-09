package com.comeoften.module.reservation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预约状态日志。表里没有操作人类型列，操作人类型写入 remark 前缀。
 */
@Data
@TableName("reservation_status_log")
public class ReservationStatusLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long reservationId;
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private String remark;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
