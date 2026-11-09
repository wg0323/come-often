package com.comeoften.module.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 改期：只换日期/时段/桌型，人数与联系人沿用原单。
 */
@Data
@Schema(description = "预约改期请求")
public class ReservationRescheduleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "预约日期不能为空")
    @Schema(description = "新预约日期，yyyy-MM-dd", required = true, example = "2026-11-13")
    private LocalDate reserveDate;

    @NotBlank(message = "时段不能为空")
    @Pattern(regexp = "^\\d{2}:\\d{2}$", message = "时段格式必须为 HH:mm")
    @Schema(description = "新时段，HH:mm", required = true, example = "19:00")
    private String timeSlot;

    @NotBlank(message = "桌型不能为空")
    @Schema(description = "新桌型", required = true, example = "PRIVATE_SMALL")
    private String tableType;
}
