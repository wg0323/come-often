package com.comeoften.module.reservation.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 预约页元数据：门店、可预约日期、时段、桌型与规则参数，页面不做业务判断。
 */
@Data
@Schema(description = "预约页元数据")
public class ReservationMetaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "门店 ID")
    private Long storeId;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "可预约未来天数，含当天")
    private Integer daysAhead;

    @Schema(description = "可预约日期，yyyy-MM-dd")
    private List<String> dates;

    @Schema(description = "开放预约的时段，HH:mm")
    private List<String> timeSlots;

    @Schema(description = "开放预约的桌型")
    private List<ReservationTableTypeOptionVO> tableTypes;

    @Schema(description = "距预约不足该小时数不可取消")
    private Long cancelLimitHours;

    @Schema(description = "预约时间过后多少分钟未签到置为未到店")
    private Integer noShowMinutes;

    @Schema(description = "下单后是否自动确认")
    private Boolean autoConfirm;
}
