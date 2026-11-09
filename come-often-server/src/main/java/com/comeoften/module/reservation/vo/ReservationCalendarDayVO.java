package com.comeoften.module.reservation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 后台日历的一格：某天的预约数量分布与容量概况。
 */
@Data
@Schema(description = "预约日历日")
public class ReservationCalendarDayVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日期，yyyy-MM-dd")
    private String date;

    @Schema(description = "预约总数")
    private Integer total;

    @Schema(description = "待确认数")
    private Integer pending;

    @Schema(description = "已确认数")
    private Integer confirmed;

    @Schema(description = "已到店数")
    private Integer checkedIn;

    @Schema(description = "未到店数")
    private Integer noShow;

    @Schema(description = "已取消数")
    private Integer cancelled;

    @Schema(description = "当日全部桌型容量之和")
    private Integer capacity;

    @Schema(description = "当日已占用桌数之和")
    private Integer booked;
}
