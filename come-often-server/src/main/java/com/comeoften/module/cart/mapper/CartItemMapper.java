package com.comeoften.module.cart.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.cart.entity.CartItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 购物车 Mapper。uk_cart_sku 唯一索引不含 is_deleted，
 * 用原生 SQL 区分「有效行累加」与「逻辑删除行复活」，避免查后插的竞态。
 */
public interface CartItemMapper extends BaseMapper<CartItem> {

    @Select("SELECT id, member_id, store_id, dish_id, spec_key, addon_keys, quantity, is_deleted, "
            + "create_time, update_time FROM cart_item "
            + "WHERE member_id = #{memberId} AND store_id = #{storeId} AND dish_id = #{dishId} "
            + "AND spec_key = #{specKey} AND addon_keys = #{addonKeys}")
    CartItem selectRawBySku(@Param("memberId") Long memberId, @Param("storeId") Long storeId,
                            @Param("dishId") Long dishId, @Param("specKey") String specKey,
                            @Param("addonKeys") String addonKeys);

    @Update("UPDATE cart_item SET quantity = quantity + #{delta}, update_time = NOW() "
            + "WHERE member_id = #{memberId} AND store_id = #{storeId} AND dish_id = #{dishId} "
            + "AND spec_key = #{specKey} AND addon_keys = #{addonKeys} AND is_deleted = 0")
    int incrementExisting(@Param("memberId") Long memberId, @Param("storeId") Long storeId,
                          @Param("dishId") Long dishId, @Param("specKey") String specKey,
                          @Param("addonKeys") String addonKeys, @Param("delta") Integer delta);

    @Update("UPDATE cart_item SET quantity = #{quantity}, is_deleted = 0, update_time = NOW() "
            + "WHERE member_id = #{memberId} AND store_id = #{storeId} AND dish_id = #{dishId} "
            + "AND spec_key = #{specKey} AND addon_keys = #{addonKeys}")
    int restoreExisting(@Param("memberId") Long memberId, @Param("storeId") Long storeId,
                        @Param("dishId") Long dishId, @Param("specKey") String specKey,
                        @Param("addonKeys") String addonKeys, @Param("quantity") Integer quantity);
}
