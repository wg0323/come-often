package com.comeoften.module.stats.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 经营看板的今日预约看板：状态计数 + 时段占用。
 */
@Data
@Schema(description = "今日预约看板")
public class StatsReservationDayVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "统计日期，yyyy-MM-dd")
    private String date;

    @Schema(description = "预约单总数，不含已取消")
    private Long total;

    @Schema(description = "待确认")
    private Long pending;

    @Schema(description = "已确认")
    private Long confirmed;

    @Schema(description = "已到店")
    private Long checkedIn;

    @Schema(description = "未到店")
    private Long noShow;

    @Schema(description = "已取消")
    private Long cancelled;

    @Schema(description = "时段占用，按时段升序")
    private List<StatsReservationSlotVO> slots;
}
