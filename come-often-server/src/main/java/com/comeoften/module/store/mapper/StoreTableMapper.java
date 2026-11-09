package com.comeoften.module.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.store.entity.StoreTable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 桌台 Mapper。uk_store_table_no 唯一索引不含 is_deleted，重新启用同名桌台时先复活旧行。
 */
public interface StoreTableMapper extends BaseMapper<StoreTable> {

    /**
     * 复活被逻辑删除的同名桌台并直接写入新配置，避免插入时撞唯一索引。
     */
    @Update("UPDATE store_table SET table_type = #{tableType}, capacity = #{capacity}, "
            + "status = #{status}, is_deleted = 0, update_time = NOW() "
            + "WHERE store_id = #{storeId} AND table_no = #{tableNo} AND is_deleted = 1")
    int restoreByNo(@Param("storeId") Long storeId, @Param("tableNo") String tableNo,
                    @Param("tableType") String tableType, @Param("capacity") Integer capacity,
                    @Param("status") Integer status);
}
