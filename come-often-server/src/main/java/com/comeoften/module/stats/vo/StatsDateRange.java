package com.comeoften.module.stats.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 内部值对象：解析后的查询区间，结束日含当天。不直接出接口（出接口用 StatsRangeVO）。
 */
@Data
public class StatsDateRange implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate startDate;

    private LocalDate endDate;

    public LocalDateTime fromInclusive() {
        return startDate.atStartOfDay();
    }

    public LocalDateTime toExclusive() {
        return endDate.plusDays(1L).atStartOfDay();
    }

    public long days() {
        return endDate.toEpochDay() - startDate.toEpochDay() + 1L;
    }
}
