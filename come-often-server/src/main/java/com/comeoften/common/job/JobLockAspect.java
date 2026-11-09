package com.comeoften.common.job;

import com.comeoften.common.JobKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * {@link JobLock} 的实现：Redis {@code SETNX + TTL} 抢任务锁，执行完用 Lua 比对 token 后释放。
 *
 * <p>三条固定口径：</p>
 * <ol>
 *   <li>抢不到锁直接跳过，不排队不重试；跳过时返回返回类型的默认值，
 *       返回类型是 {@link JobResult} 时返回 {@code executed=false} 的实例，便于运维端点观测。</li>
 *   <li>释放用 Lua 做「比对 + 删除」原子操作（{@code lua/job_lock_release.lua}）：
 *       任务耗时超过 TTL 时，GET 与 DEL 之间不会插进别的实例刚拿到的锁。同理不续期——
 *       TTL 应该由调用方按最长耗时给足，而不是靠续期兜底。</li>
 *   <li>Redis 不可用时按 fail-open 处理：打 error 日志后照常执行。兜底任务都按 DB 条件幂等
 *       （状态 CAS、唯一索引、比对后写入），一次 Redis 抖动不该让兜底能力整体停摆。</li>
 * </ol>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class JobLockAspect {

    private final StringRedisTemplate stringRedisTemplate;

    private final DefaultRedisScript<Long> jobLockReleaseScript;

    @Around("@annotation(jobLock)")
    public Object around(ProceedingJoinPoint point, JobLock jobLock) throws Throwable {
        String jobName = resolveJobName(point, jobLock);
        String lockKey = JobKeys.lock(jobName);
        String token = UUID.randomUUID().toString().replace("-", "");
        boolean acquired = tryAcquire(lockKey, token, jobLock.ttlSeconds(), jobName);
        if (!acquired) {
            log.info("任务锁被占用，本次跳过: job={}, lock={}", jobName, lockKey);
            return skippedValue(point, jobName);
        }
        try {
            return point.proceed();
        } finally {
            release(lockKey, token, jobName);
        }
    }

    private boolean tryAcquire(String lockKey, String token, long ttlSeconds, String jobName) {
        try {
            Boolean locked = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, token, ttlSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(locked);
        } catch (RuntimeException e) {
            log.error("任务锁获取失败，本次无锁执行: job={}, lock={}", jobName, lockKey, e);
            return true;
        }
    }

    private void release(String lockKey, String token, String jobName) {
        try {
            stringRedisTemplate.execute(jobLockReleaseScript, Collections.singletonList(lockKey), token);
        } catch (RuntimeException e) {
            log.warn("任务锁释放失败，等待 TTL 自动过期: job={}, lock={}", jobName, lockKey, e);
        }
    }

    private static String resolveJobName(ProceedingJoinPoint point, JobLock jobLock) {
        if (StringUtils.hasText(jobLock.key())) {
            return jobLock.key();
        }
        return ((MethodSignature) point.getSignature()).getMethod().getName();
    }

    /**
     * 跳过时的返回值：{@link JobResult} 返回「未执行」实例，基本类型返回零值，其余返回 null。
     */
    private static Object skippedValue(ProceedingJoinPoint point, String jobName) {
        Class<?> returnType = ((MethodSignature) point.getSignature()).getReturnType();
        if (returnType == void.class) {
            return null;
        }
        if (JobResult.class.isAssignableFrom(returnType)) {
            return JobResult.skipped(jobName);
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == boolean.class) {
            return Boolean.FALSE;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == float.class) {
            return 0F;
        }
        return null;
    }
}
