package com.comeoften.module.dish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.dish.entity.DishTagRel;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 标签关联 Mapper。uk_dish_tag 唯一索引与逻辑删除共存，重新打标时先复活旧记录再插入。
 */
public interface DishTagRelMapper extends BaseMapper<DishTagRel> {

    /**
     * 复活曾被逻辑删除的标签关联，避免重复插入撞唯一索引。
     */
    @Update("UPDATE dish_tag_rel SET is_deleted = 0, update_time = NOW() "
            + "WHERE dish_id = #{dishId} AND tag_id = #{tagId} AND is_deleted = 1")
    int revive(@Param("dishId") Long dishId, @Param("tagId") Long tagId);
}
