package com.comeoften.module.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 顾客端创建预约。人数、桌型与联络方式由前端提交，容量与时段合法性一律由服务端校验。
 */
@Data
@Schema(description = "创建预约请求")
public class ReservationCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "预约日期不能为空")
    @Schema(description = "预约日期，yyyy-MM-dd", required = true, example = "2026-11-12")
    private LocalDate reserveDate;

    @NotBlank(message = "时段不能为空")
    @Pattern(regexp = "^\\d{2}:\\d{2}$", message = "时段格式必须为 HH:mm")
    @Schema(description = "时段，HH:mm", required = true, example = "18:00")
    private String timeSlot;

    @NotBlank(message = "桌型不能为空")
    @Schema(description = "桌型：HALL_CARD / PRIVATE_SMALL / PRIVATE_LARGE / WINDOW", required = true,
            example = "PRIVATE_SMALL")
    private String tableType;

    @NotNull(message = "就餐人数不能为空")
    @Min(value = 1, message = "就餐人数至少 1 人")
    @Max(value = 20, message = "就餐人数最多 20 人")
    @Schema(description = "就餐人数", required = true, example = "8")
    private Integer peopleCount;

    @NotBlank(message = "联系人不能为空")
    @Size(max = 64, message = "联系人不超过 64 字")
    @Schema(description = "联系人", required = true, example = "张先生")
    private String contactName;

    @NotBlank(message = "联系手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不合法")
    @Schema(description = "联系手机号", required = true, example = "13800138000")
    private String contactPhone;

    @Size(max = 255, message = "备注不超过 255 字")
    @Schema(description = "备注：生日布置 / 儿童座椅 / 忌口")
    private String remark;

    @Schema(description = "包间类型筛选：HALL / PRIVATE_SMALL / PRIVATE_LARGE，不指定具体包间时可空")
    private String roomType;

    @Schema(description = "指定具体包间的桌台 ID；为空表示只按桌型容量桶占座")
    private Long tableId;
}
