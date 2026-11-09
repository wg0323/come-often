package com.comeoften.module.reservation.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预约状态日志，供后台详情回溯。
 */
@Data
@Schema(description = "预约状态日志")
public class ReservationStatusLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Schema(description = "迁移前状态")
    private String fromStatus;

    @Schema(description = "迁移前状态中文说明")
    private String fromStatusText;

    @Schema(description = "迁移后状态")
    private String toStatus;

    @Schema(description = "迁移后状态中文说明")
    private String toStatusText;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "操作人 ID，系统操作时为空")
    private Long operatorId;

    @Schema(description = "说明")
    private String remark;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "发生时间")
    private LocalDateTime createTime;
}
