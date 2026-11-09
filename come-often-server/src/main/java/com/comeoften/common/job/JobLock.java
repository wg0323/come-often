package com.comeoften.common.job;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 兜底任务的单实例互斥锁（计划 3.10）。锁粒度是任务名：同一 {@link #key()} 在同一时刻只允许一个执行者。
 *
 * <p>抢不到锁的行为固定为「跳过」——不排队、不阻塞、不重试。兜底任务的下一个周期会自己再来一次，
 * 排队只会把调度线程堵住。跳过时按返回类型的默认值返回（{@code int} 为 0、对象为 {@code null}、
 * {@code void} 为无返回），与 M6 起「其他实例正在跑时返回 0」的口径一致。</p>
 *
 * <p>实现见 {@link JobLockAspect}。{@code ttlSeconds} 必须明显大于任务最长耗时：
 * 锁超过 TTL 会被 Redis 自动回收，此时释放逻辑可能误删其他实例刚拿到的锁。</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JobLock {

    /**
     * 任务名，作为锁键后缀：{@code comeoften:job:lock:{key}}。留空时取方法名。
     */
    String key() default "";

    /**
     * 锁的存活秒数，超时自动释放。默认 5 分钟，需按任务实际耗时调整。
     */
    long ttlSeconds() default 300L;
}
