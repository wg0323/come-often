package com.comeoften.module.stats.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 经营看板上的时段占用：同一个时段把各桌型的容量与已订桌数相加，
 * 详情仍去预约管理页看分桌型的容量桶。
 */
@Data
@Schema(description = "预约时段占用")
public class StatsReservationSlotVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "时段，HH:mm")
    private String timeSlot;

    @Schema(description = "该时段可用桌数合计")
    private Integer capacity;

    @Schema(description = "该时段已订桌数合计")
    private Integer booked;
}
