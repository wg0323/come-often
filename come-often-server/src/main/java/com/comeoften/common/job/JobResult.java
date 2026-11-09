package com.comeoften.common.job;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 兜底任务的执行结果。既供运维端点返回，也让「抢不到锁而跳过」可被外部观测：
 * {@code executed=false} 表示本次调用没有真正执行（锁被其他实例持有）。
 */
@Data
@Schema(description = "定时任务执行结果")
public class JobResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "任务名，对应 @JobLock 的 key")
    private String jobName;

    @Schema(description = "本次是否真正执行；false 表示锁被其他实例持有而跳过")
    private boolean executed;

    @Schema(description = "本次处理条数；该任务没有计数语义时为 null")
    private Integer count;

    @Schema(description = "执行摘要，仅含 ASCII 与数字，供日志与验收核对")
    private String detail;

    public static JobResult executed(String jobName, int count) {
        return build(jobName, true, count, "count=" + count);
    }

    public static JobResult executed(String jobName, int count, String detail) {
        return build(jobName, true, count, detail);
    }

    public static JobResult skipped(String jobName) {
        return build(jobName, false, null, "skipped");
    }

    /**
     * 执行失败（异常已在本任务内记录）。{@code executed=false} 同时覆盖「被锁跳过」与「执行失败」，
     * 用 {@code detail} 区分——不要用 true + count=0 冒充成功。
     */
    public static JobResult failed(String jobName, String detail) {
        return build(jobName, false, null, "failed:" + detail);
    }

    private static JobResult build(String jobName, boolean executed, Integer count, String detail) {
        JobResult result = new JobResult();
        result.setJobName(jobName);
        result.setExecuted(executed);
        result.setCount(count);
        result.setDetail(detail);
        return result;
    }
}
