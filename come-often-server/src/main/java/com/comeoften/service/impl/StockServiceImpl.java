package com.comeoften.service.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis Lua 预扣实现。执行顺序固定：Lua 预扣成功 → 业务 DB 事务 → 任一步失败 rollback 回补 Redis。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private static final String MODE_DEDUCT = "DEDUCT";
    private static final String MODE_OCCUPY = "OCCUPY";
    private static final String MODE_DEDUCT_ROLLBACK = "DEDUCT_ROLLBACK";
    private static final String MODE_OCCUPY_ROLLBACK = "OCCUPY_ROLLBACK";
    private static final long SUCCESS = 1L;
    private static final String NO_CAPACITY = "0";

    /**
     * 库存 key 兜底 TTL。每日 00:10 会按 DB 真实值重建，48 小时足够覆盖两次重建，
     * 同时保证业务记录已删除时不会留下永久漂移的幽灵 key。
     */
    private static final long STOCK_KEY_TTL_HOURS = 48L;

    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> stockDeductScript;

    @Override
    public boolean tryDeduct(String key, int count) {
        return execute(key, MODE_DEDUCT, count, NO_CAPACITY);
    }

    @Override
    public boolean tryOccupy(String key, int count, int capacity) {
        return execute(key, MODE_OCCUPY, count, String.valueOf(capacity));
    }

    @Override
    public void rollback(String key, int count, boolean occupy) {
        execute(key, occupy ? MODE_OCCUPY_ROLLBACK : MODE_DEDUCT_ROLLBACK, count, NO_CAPACITY);
    }

    @Override
    public void reset(String key, long value) {
        checkKey(key);
        stringRedisTemplate.opsForValue().set(key, String.valueOf(value),
                STOCK_KEY_TTL_HOURS, TimeUnit.HOURS);
        log.info("库存 key 已按 DB 重建: key={}, value={}, ttlHours={}", key, value, STOCK_KEY_TTL_HOURS);
    }

    @Override
    public void initIfAbsent(String key, long value) {
        checkKey(key);
        Boolean created = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, String.valueOf(value), STOCK_KEY_TTL_HOURS, TimeUnit.HOURS);
        if (Boolean.TRUE.equals(created)) {
            log.info("库存 key 已按 DB 初始化: key={}, value={}, ttlHours={}",
                    key, value, STOCK_KEY_TTL_HOURS);
        }
    }

    @Override
    public long remaining(String key) {
        checkKey(key);
        String value = stringRedisTemplate.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value);
    }

    private boolean execute(String key, String mode, int count, String capacity) {
        checkKey(key);
        if (count <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "库存数量必须大于 0");
        }
        Long result = stringRedisTemplate.execute(stockDeductScript, Collections.singletonList(key),
                mode, String.valueOf(count), capacity);
        if (result == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "库存脚本未返回结果");
        }
        if (result == -1L) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "库存脚本收到非法模式");
        }
        if (result != SUCCESS) {
            log.info("库存预扣失败: key={}, mode={}, count={}", key, mode, count);
            return false;
        }
        return true;
    }

    private static void checkKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "库存键不能为空");
        }
    }
}
