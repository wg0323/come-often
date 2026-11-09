package com.comeoften.module.reservation.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约单。reserve_no 同时用于签到码，具体桌号在确认或签到时才绑定。
 */
@Data
@TableName("reservation")
public class Reservation implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String reserveNo;
    private Long storeId;
    private Long memberId;
    private LocalDate reserveDate;
    private String timeSlot;
    private String tableType;
    private Integer peopleCount;
    private String contactName;
    private String contactPhone;
    private String remark;
    private String status;
    private Long tableId;
    private Long checkinUserId;
    private LocalDateTime checkinTime;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
