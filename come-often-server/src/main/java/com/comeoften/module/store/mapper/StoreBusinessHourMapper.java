package com.comeoften.module.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.store.entity.StoreBusinessHour;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 门店营业时间 Mapper。uk_store_week 唯一索引不含 is_deleted，复活旧行后再更新。
 */
public interface StoreBusinessHourMapper extends BaseMapper<StoreBusinessHour> {

    /**
     * 复活曾被逻辑删除的星期配置。
     */
    @Update("UPDATE store_business_hour SET is_deleted = 0, update_time = NOW() "
            + "WHERE store_id = #{storeId} AND week_day = #{weekDay} AND is_deleted = 1")
    int restore(@Param("storeId") Long storeId, @Param("weekDay") Integer weekDay);
}
