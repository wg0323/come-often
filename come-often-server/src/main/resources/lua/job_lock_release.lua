-- 任务锁释放：只有值仍是本次运行的 token 才删除，避免任务超过 TTL 后误删别人刚拿到的锁。
-- KEYS[1] = 锁键；ARGV[1] = 本次运行的 token。返回 1 表示删掉了自己的锁，0 表示已不是自己的锁。
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
