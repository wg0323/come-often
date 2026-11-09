package com.comeoften.service;

/**
 * 库存预扣。三类库存共用同一套机制：优惠券模板库存、菜品每日库存用余量递减；
 * 预约时段容量用占用递增。
 */
public interface StockService {

    /**
     * 余量型预扣，成功返回 true。
     */
    boolean tryDeduct(String key, int count);

    /**
     * 占用型预扣，成功返回 true。
     */
    boolean tryOccupy(String key, int count, int capacity);

    /**
     * 反向回补 Redis。业务 DB 事务失败时调用。
     */
    void rollback(String key, int count, boolean occupy);

    /**
     * 按 DB 真实值重建 key，供每日 00:10 兜底任务调用。
     */
    void reset(String key, long value);

    /**
     * 仅在 key 不存在时按 DB 真实值初始化，避免并发重建覆盖已发生的预扣。
     */
    void initIfAbsent(String key, long value);

    /**
     * 读取当前值，供核对使用。
     */
    long remaining(String key);
}
