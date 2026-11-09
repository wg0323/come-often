package com.comeoften.module.reservation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 桌台看板：桌台三态 + 当日时段容量 + 当日预约明细。
 */
@Data
@Schema(description = "桌台看板")
public class ReservationTableBoardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日期，yyyy-MM-dd")
    private String date;

    @Schema(description = "桌台列表")
    private List<TableBoardItemVO> tables;

    @Schema(description = "当日时段容量")
    private List<ReservationSlotVO> slots;

    @Schema(description = "当日预约明细")
    private List<ReservationVO> reservations;
}
