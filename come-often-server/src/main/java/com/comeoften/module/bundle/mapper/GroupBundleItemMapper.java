package com.comeoften.module.bundle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.bundle.entity.GroupBundleItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 套餐明细。编辑是整体覆盖式，直接物理删除旧明细再插入：
 * 明细是套餐配置的从表、没有业务单据引用它的主键，订单里存的是独立快照。
 */
public interface GroupBundleItemMapper extends BaseMapper<GroupBundleItem> {

    /**
     * 按套餐查明细；显式列名，不带 is_deleted 过滤的复活由「整体覆盖」代替。
     */
    @Select("SELECT id, bundle_id, dish_id, dish_name, quantity, sort, is_deleted, create_time, update_time "
            + "FROM group_bundle_item WHERE bundle_id = #{bundleId} AND is_deleted = 0 "
            + "ORDER BY sort, id")
    List<GroupBundleItem> selectByBundle(@Param("bundleId") Long bundleId);

    @Delete("DELETE FROM group_bundle_item WHERE bundle_id = #{bundleId}")
    int deleteByBundle(@Param("bundleId") Long bundleId);
}
