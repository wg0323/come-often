package com.comeoften.module.reservation.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 可指定包间。available 由「该日期时段是否已有有效预约占用」判定。
 */
@Data
@Schema(description = "可指定包间")
public class ReservationRoomVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "桌台 ID，创建预约时作为 tableId 传入")
    private Long tableId;

    @Schema(description = "桌号 / 包间名")
    private String tableNo;

    @Schema(description = "包间类型：HALL/PRIVATE_SMALL/PRIVATE_LARGE")
    private String roomType;

    @Schema(description = "包间类型文案")
    private String roomTypeText;

    @Schema(description = "桌型")
    private String tableType;

    @Schema(description = "可容纳人数")
    private Integer capacity;
}
