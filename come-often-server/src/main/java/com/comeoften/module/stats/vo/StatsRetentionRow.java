package com.comeoften.module.stats.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 内部聚合行：次日留存。cohortSize 是区间内首单且次日已完整结束的会员数。
 */
@Data
public class StatsRetentionRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long cohortSize;

    private Long retainedCount;
}
