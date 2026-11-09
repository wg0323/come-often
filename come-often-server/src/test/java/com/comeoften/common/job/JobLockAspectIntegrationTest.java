package com.comeoften.common.job;

import com.comeoften.common.JobKeys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 直连本地 Redis 6.2.6 容器验证 @JobLock 的互斥语义：并发触发同一任务只有一次真正执行，
 * 任务异常也释放锁，锁被别人接手时不会误删。使用独立任务名，结束后清理锁键。
 */
class JobLockAspectIntegrationTest {

    private static final String JOB_NAME = "it-job-lock";

    private static final String LOCK_KEY = "comeoften:job:lock:" + JOB_NAME;

    private static final int THREAD_COUNT = 8;

    private LettuceConnectionFactory connectionFactory;

    private StringRedisTemplate redisTemplate;

    private LockedTask task;

    private LockedTask proxy;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory("127.0.0.1", 6379);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        redisTemplate.delete(Collections.singletonList(LOCK_KEY));
        task = new LockedTask(redisTemplate);
        AspectJProxyFactory factory = new AspectJProxyFactory(task);
        factory.setProxyTargetClass(true);
        factory.addAspect(new JobLockAspect(redisTemplate, jobLockReleaseScript()));
        proxy = (LockedTask) factory.getProxy();
    }

    @AfterEach
    void tearDown() {
        redisTemplate.delete(Collections.singletonList(LOCK_KEY));
        connectionFactory.destroy();
    }

    @Test
    void should_execute_once_when_concurrent_trigger() throws InterruptedException {
        AtomicInteger executed = new AtomicInteger(0);
        AtomicInteger skipped = new AtomicInteger(0);
        AtomicInteger noResult = new AtomicInteger(0);
        runConcurrently(() -> {
            JobResult result = proxy.run();
            if (result == null) {
                noResult.incrementAndGet();
            } else if (result.isExecuted()) {
                executed.incrementAndGet();
            } else {
                skipped.incrementAndGet();
            }
        });
        Assertions.assertEquals(1, task.invocations.get(), "任务体只能被执行一次");
        Assertions.assertEquals(1, executed.get(), "只有一次返回 executed=true");
        Assertions.assertEquals(THREAD_COUNT - 1, skipped.get(), "其余调用应返回 executed=false");
        Assertions.assertEquals(0, noResult.get(), "跳过时也应返回 JobResult，不能是 null");
        Assertions.assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_KEY)), "执行结束后锁应被释放");
    }

    @Test
    void should_release_lock_when_task_throws() {
        Assertions.assertThrows(IllegalStateException.class, proxy::fail);
        Assertions.assertFalse(Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_KEY)),
                "任务异常也要释放锁，否则下一轮被永久挡住");
    }

    @Test
    void should_not_delete_lock_owned_by_another_instance() {
        proxy.stealLockInsideTask();
        Assertions.assertEquals("other-instance", redisTemplate.opsForValue().get(LOCK_KEY),
                "锁的值已被别的实例改写时，释放逻辑不能删掉对方的锁");
    }

    @Test
    void should_use_method_name_when_key_is_blank() {
        Assertions.assertEquals("comeoften:job:lock:" + "runWithoutKey",
                JobKeys.lock("runWithoutKey"), "key 留空时以方法名为锁名");
    }

    private static DefaultRedisScript<Long> jobLockReleaseScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/job_lock_release.lua")));
        script.setResultType(Long.class);
        return script;
    }

    private void runConcurrently(Runnable runnable) throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(THREAD_COUNT, THREAD_COUNT,
                0L, TimeUnit.MILLISECONDS, new java.util.concurrent.LinkedBlockingQueue<>(THREAD_COUNT),
                new java.util.concurrent.ThreadFactory() {
                    private final AtomicInteger index = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "job-lock-test-" + index.getAndIncrement());
                    }
                });
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.execute(() -> {
                try {
                    start.await();
                    runnable.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        Assertions.assertTrue(done.await(15, TimeUnit.SECONDS), "并发任务应在 15 秒内完成");
        executor.shutdownNow();
    }

    /**
     * 被切面代理的任务体：持锁 300ms，保证并发调用窗口真实重叠。
     */
    static class LockedTask {

        private final StringRedisTemplate redisTemplate;

        private final AtomicInteger invocations = new AtomicInteger(0);

        LockedTask(StringRedisTemplate redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        @JobLock(key = JOB_NAME, ttlSeconds = 30L)
        public JobResult run() {
            invocations.incrementAndGet();
            try {
                Thread.sleep(300L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return JobResult.executed(JOB_NAME, 1);
        }

        @JobLock(key = JOB_NAME, ttlSeconds = 30L)
        public JobResult fail() {
            throw new IllegalStateException("boom");
        }

        /**
         * 模拟「任务耗时超过 TTL，锁被另一个实例重新抢到」的场景。
         */
        @JobLock(key = JOB_NAME, ttlSeconds = 30L)
        public JobResult stealLockInsideTask() {
            redisTemplate.opsForValue().set(JobKeys.lock(JOB_NAME), "other-instance", 30L, TimeUnit.SECONDS);
            return JobResult.executed(JOB_NAME, 1);
        }
    }
}
