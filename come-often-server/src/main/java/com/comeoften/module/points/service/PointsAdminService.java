package com.comeoften.module.points.service;

import com.comeoften.common.PageResult;
import com.comeoften.module.points.vo.PointsMemberVO;
import com.comeoften.module.points.vo.PointsRecordVO;

/**
 * PC 后台的积分查询。写操作（等级配置）在 PointsLevelService，过期批处理在 PointsAccountService。
 */
public interface PointsAdminService {

    /**
     * 会员积分分页，按注册时间倒序；keyword 命中昵称或手机号。
     */
    PageResult<PointsMemberVO> memberPage(Long current, Long size, String keyword);

    /**
     * 按会员标签筛选（如 SLEEPING），tagCode 为空时不加条件。
     */
    PageResult<PointsMemberVO> memberPage(Long current, Long size, String keyword, String tagCode);

    /**
     * 指定会员的积分流水分页；memberId 为空时查全店流水。
     */
    PageResult<PointsRecordVO> recordPage(Long memberId, Long current, Long size);
}
