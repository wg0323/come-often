package com.comeoften.service.impl;

import com.comeoften.service.StockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * 直连本地 Redis 6.2.6 容器验证 Lua 原子性，使用独立测试 key 前缀，结束后清理。
 */
class StockServiceIntegrationTest {

    private static final String TEST_KEY = "comeoften:test:stock:deduct";
    private static final String OCCUPY_KEY = "comeoften:test:stock:occupy";
    private static final int THREAD_COUNT = 20;
    private static final int CAPACITY = 5;

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private StockService stockService;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory("127.0.0.1", 6379);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/stock_deduct.lua")));
        script.setResultType(Long.class);
        stockService = new StockServiceImpl(redisTemplate, script);
        stockService.reset(TEST_KEY, CAPACITY);
        stockService.reset(OCCUPY_KEY, 0L);
    }

    @AfterEach
    void tearDown() {
        redisTemplate.delete(Collections.singletonList(TEST_KEY));
        redisTemplate.delete(Collections.singletonList(OCCUPY_KEY));
        connectionFactory.destroy();
    }

    @Test
    void should_not_oversell_when_concurrent_deduct() throws InterruptedException {
        AtomicInteger success = new AtomicInteger(0);
        runConcurrently(() -> {
            if (stockService.tryDeduct(TEST_KEY, 1)) {
                success.incrementAndGet();
            }
        });
        Assertions.assertEquals(CAPACITY, success.get(), "并发扣减成功数应恰好等于库存");
        Assertions.assertEquals(0L, stockService.remaining(TEST_KEY), "余量应扣至 0");
    }

    @Test
    void should_not_overbook_when_concurrent_occupy() throws InterruptedException {
        AtomicInteger success = new AtomicInteger(0);
        runConcurrently(() -> {
            if (stockService.tryOccupy(OCCUPY_KEY, 1, CAPACITY)) {
                success.incrementAndGet();
            }
        });
        Assertions.assertEquals(CAPACITY, success.get(), "并发预约成功数应恰好等于时段容量");
        Assertions.assertEquals(CAPACITY, (int) stockService.remaining(OCCUPY_KEY));
    }

    @Test
    void should_restore_stock_when_rollback() {
        Assertions.assertTrue(stockService.tryDeduct(TEST_KEY, 2));
        stockService.rollback(TEST_KEY, 2, false);
        Assertions.assertEquals(CAPACITY, (int) stockService.remaining(TEST_KEY));
    }

    private void runConcurrently(Runnable task) throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(THREAD_COUNT, THREAD_COUNT,
                0L, TimeUnit.MILLISECONDS, new java.util.concurrent.LinkedBlockingQueue<>(THREAD_COUNT),
                new java.util.concurrent.ThreadFactory() {
                    private final AtomicInteger index = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable runnable) {
                        return new Thread(runnable, "stock-test-" + index.getAndIncrement());
                    }
                });
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.execute(() -> {
                try {
                    start.await();
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        Assertions.assertTrue(done.await(10, TimeUnit.SECONDS), "并发任务应在 10 秒内完成");
        executor.shutdownNow();
    }
}
