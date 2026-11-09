-- 三类库存共用的原子脚本，Redis 只作快速判定层，DB 是最终事实源。
-- KEYS[1] 库存键
-- ARGV[1] 模式：DEDUCT(余量递减) / OCCUPY(占用递增) / DEDUCT_ROLLBACK / OCCUPY_ROLLBACK
-- ARGV[2] 数量 n
-- ARGV[3] 容量 cap，仅 OCCUPY 使用
-- 返回：1 成功，0 库存不足/容量已满，-1 非法模式
-- 回滚分支也要补 TTL，否则 key 已过期时回滚会创建永不过期的幽灵键。
local mode = ARGV[1]
local n = tonumber(ARGV[2])
local key = KEYS[1]
local cur = tonumber(redis.call('get', key) or '0')
local ttl_seconds = 172800

if mode == 'DEDUCT' then
    if cur >= n then
        redis.call('decrby', key, n)
        return 1
    end
    return 0
elseif mode == 'OCCUPY' then
    local cap = tonumber(ARGV[3])
    if cur + n <= cap then
        redis.call('incrby', key, n)
        return 1
    end
    return 0
elseif mode == 'DEDUCT_ROLLBACK' then
    redis.call('incrby', key, n)
    redis.call('expire', key, ttl_seconds)
    return 1
elseif mode == 'OCCUPY_ROLLBACK' then
    redis.call('decrby', key, n)
    redis.call('expire', key, ttl_seconds)
    return 1
end

return -1
