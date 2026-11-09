package com.comeoften.module.reservation.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 时段容量。capacity 是该桌型桌数，booked 是已占桌数，remaining 由服务端算好。
 */
@Data
@Schema(description = "预约时段容量")
public class ReservationSlotVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String DATE_PATTERN = "yyyy-MM-dd";

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "时段 ID")
    private Long slotId;

    @JsonFormat(pattern = DATE_PATTERN)
    @Schema(description = "预约日期")
    private LocalDate reserveDate;

    @Schema(description = "时段，HH:mm")
    private String timeSlot;

    @Schema(description = "桌型编码")
    private String tableType;

    @Schema(description = "桌型中文说明")
    private String tableTypeText;

    @Schema(description = "容量，即该桌型桌数")
    private Integer capacity;

    @Schema(description = "已预约桌数")
    private Integer booked;

    @Schema(description = "剩余可预约桌数")
    private Integer remaining;

    @Schema(description = "是否已满")
    private Boolean full;
}
