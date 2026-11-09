package com.comeoften.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.auth.entity.Member;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 顾客会员 Mapper。 */
public interface MemberMapper extends BaseMapper<Member> {

    /**
     * 只升不降地绑定会员等级：目标等级的排序值必须大于会员当前等级的排序值。
     *
     * <p>守卫写在 SQL 里而不是 Java 侧比大小——并发消费时「先读快照再比」会拿到过期结论，
     * 两个线程可能把更高等级改回低等级。</p>
     */
    @Update("UPDATE member m SET m.level_id = #{levelId}, m.update_time = NOW() "
            + "WHERE m.id = #{memberId} AND m.is_deleted = 0 "
            + "AND (m.level_id IS NULL OR EXISTS (SELECT 1 FROM member_level lv "
            + "WHERE lv.id = m.level_id AND lv.is_deleted = 0 AND lv.sort < #{targetSort}))")
    int upgradeLevel(@Param("memberId") Long memberId, @Param("levelId") Long levelId,
                     @Param("targetSort") Integer targetSort);
}
