package com.comeoften.module.stats.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 本次报表实际生效的查询区间与粒度，回给页面用于展示与二次导出。
 */
@Data
@Schema(description = "报表查询区间")
public class StatsRangeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "起始日，yyyy-MM-dd")
    private String startDate;

    @Schema(description = "结束日，yyyy-MM-dd，含当天")
    private String endDate;

    @Schema(description = "时间粒度编码：DAY / WEEK / MONTH")
    private String granularity;

    @Schema(description = "时间粒度中文说明")
    private String granularityText;

    @Schema(description = "热销 / 高价值用户排行榜取前 N 条")
    private Integer topN;
}
