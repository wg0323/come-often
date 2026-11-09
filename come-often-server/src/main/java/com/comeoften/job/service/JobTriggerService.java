package com.comeoften.job.service;

import com.comeoften.common.job.JobResult;

import java.util.List;

/**
 * 兜底任务的统一触发入口。调度器（cron）与运维端点（手动补跑）都走这里，
 * 因此两条路径共享同一个 @JobLock，不会并发执行同一个任务。
 */
public interface JobTriggerService {

    /**
     * 按任务名执行一次。任务名不存在抛 10001；锁被占用时返回 {@code executed=false}。
     */
    JobResult run(String jobName);

    /**
     * 已注册的任务名，供后台下拉与验收核对。
     */
    List<String> names();
}
