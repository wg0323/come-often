package com.comeoften.module.reservation.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 桌台看板的一格。status 沿用 store_table 的三态，reservationCount 是当日该桌型的预约数。
 */
@Data
@Schema(description = "桌台看板项")
public class TableBoardItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "桌台 ID")
    private Long tableId;

    @Schema(description = "桌号或包间名")
    private String tableNo;

    @Schema(description = "桌型编码")
    private String tableType;

    @Schema(description = "桌型中文说明")
    private String tableTypeText;

    @Schema(description = "可容纳人数")
    private Integer capacity;

    @Schema(description = "状态：0-空闲，1-占用，2-已预约")
    private Integer status;

    @Schema(description = "状态中文说明")
    private String statusText;

    @Schema(description = "当日该桌型的预约数")
    private Integer reservationCount;

    @Schema(description = "该桌型最近一个预约时段，无预约时为空")
    private String nextTimeSlot;
}
