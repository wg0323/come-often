package com.comeoften.module.bundle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.bundle.entity.GroupBundle;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 团购套餐。库存扣减与回补都用带条件的原生 UPDATE，避免「先查再改」在并发下超卖。
 */
public interface GroupBundleMapper extends BaseMapper<GroupBundle> {

    /**
     * 下单扣库存：只有上架、未删除且余量足够时才扣成功。
     */
    @Update("UPDATE group_bundle SET remain_stock = remain_stock - #{quantity}, update_time = NOW() "
            + "WHERE id = #{id} AND is_deleted = 0 AND status = 1 AND remain_stock >= #{quantity}")
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);

    /**
     * 取消 / 超时回补库存，与订单状态 CAS 同事务，重复取消不会二次加回。
     */
    @Update("UPDATE group_bundle SET remain_stock = remain_stock + #{quantity}, update_time = NOW() "
            + "WHERE id = #{id} AND is_deleted = 0 "
            + "AND remain_stock + #{quantity} <= total_stock")
    int restoreStock(@Param("id") Long id, @Param("quantity") int quantity);
}
