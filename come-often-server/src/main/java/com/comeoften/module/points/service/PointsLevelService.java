package com.comeoften.module.points.service;

import com.comeoften.module.points.dto.MemberLevelSaveDTO;
import com.comeoften.module.points.vo.MemberLevelOverviewVO;
import com.comeoften.module.points.vo.MemberLevelVO;

import java.util.List;

/**
 * 会员等级。门槛来自 member_level，升降级只在订单完成消费里计算，不单开定时任务（计划 3.7）。
 */
public interface PointsLevelService {

    /**
     * 等级页数据：当前等级、全部档位与距下一档的差值。
     */
    MemberLevelOverviewVO overview(Long memberId);

    /**
     * 重新判定并升级会员等级，返回升级后的等级；未升级返回 null。
     */
    MemberLevelVO refreshLevel(Long memberId);

    /**
     * 全部等级，按排序值升序。
     */
    List<MemberLevelVO> listLevels();

    /**
     * 后台编辑等级门槛与折扣率。
     */
    MemberLevelVO updateLevel(Long levelId, MemberLevelSaveDTO dto);
}
