package com.comeoften.job;

import com.comeoften.common.job.JobLock;
import com.comeoften.common.job.JobResult;
import com.comeoften.module.member.constant.MemberTagCode;
import com.comeoften.module.member.service.MemberTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 沉睡会员打标（M14 B3）：每日 00:30 按 {@code comeoften.member.sleeping-days} 刷新 {@code member_tag}。
 *
 * <p>解决的正是 M11 已知局限里的「阈值没定、没有读路径所以不做」：现在阈值走配置，
 * 后台会员列表可按 {@code SLEEPING} 筛选，{@code coupon_grant_rule} 的沉睡唤醒规则也读同一批标签。</p>
 *
 * <p>幂等：打标前查重、摘标按标签行 ID 逻辑删除；重复执行只会得到 {@code marked=0, cleared=0}。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberTagJob {

    public static final String JOB_NAME = "member-tag";

    private static final long LOCK_TTL_SECONDS = 300L;

    private final MemberTagService memberTagService;

    @JobLock(key = JOB_NAME, ttlSeconds = LOCK_TTL_SECONDS)
    public JobResult refresh() {
        int changed = memberTagService.refreshSleeping();
        log.info("沉睡标签任务完成: tag={}, changed={}", MemberTagCode.SLEEPING, changed);
        return JobResult.executed(JOB_NAME, changed, "tag=" + MemberTagCode.SLEEPING + ",changed=" + changed);
    }
}
