package com.comeoften.module.reservation.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约单。canCancel / canCheckin 由服务端按当前时间与状态算好，页面不重复判断。
 */
@Data
@Schema(description = "预约单")
public class ReservationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private static final String DATE_PATTERN = "yyyy-MM-dd";

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "预约 ID")
    private Long id;

    @Schema(description = "预约单号，同时用于签到码")
    private String reserveNo;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "门店 ID")
    private Long storeId;

    @Schema(description = "门店名称")
    private String storeName;

    @JsonFormat(pattern = DATE_PATTERN)
    @Schema(description = "预约日期")
    private LocalDate reserveDate;

    @Schema(description = "时段，HH:mm")
    private String timeSlot;

    @Schema(description = "桌型编码")
    private String tableType;

    @Schema(description = "桌型中文说明")
    private String tableTypeText;

    @Schema(description = "就餐人数")
    private Integer peopleCount;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系手机号")
    private String contactPhone;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态：PENDING/CONFIRMED/CHECKED_IN/NO_SHOW/CANCELLED")
    private String status;

    @Schema(description = "状态中文说明")
    private String statusText;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "签到时绑定的具体桌台 ID")
    private Long tableId;

    @Schema(description = "签到时绑定的具体桌号")
    private String tableNo;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "签到操作员工 ID")
    private Long checkinUserId;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "签到时间")
    private LocalDateTime checkinTime;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "预约时刻，由日期与时段拼出")
    private LocalDateTime reserveAt;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "会员昵称，仅后台查询返回")
    private String memberNickname;

    @Schema(description = "会员手机号，仅后台查询返回")
    private String memberPhone;

    @Schema(description = "当前是否可取消")
    private Boolean canCancel;

    @Schema(description = "当前是否可签到")
    private Boolean canCheckin;

    @Schema(description = "状态日志，仅详情接口返回")
    private List<ReservationStatusLogVO> logs;
}
