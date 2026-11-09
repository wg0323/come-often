package com.comeoften.module.coupon.service;

import com.comeoften.common.PageResult;
import com.comeoften.module.coupon.dto.CouponGrantDTO;
import com.comeoften.module.coupon.dto.CouponTemplateSaveDTO;
import com.comeoften.module.coupon.vo.CouponOperatorVO;
import com.comeoften.module.coupon.vo.CouponRoiVO;
import com.comeoften.module.coupon.vo.CouponTemplateVO;
import com.comeoften.module.coupon.vo.CouponUseRecordVO;

import java.time.LocalDate;
import java.util.List;

/**
 * PC 后台营销管理：券模板 CRUD、定向发券、核销记录与券 ROI。
 * 核销本身走 CouponVerifyService，后台只负责录入与查询。
 */
public interface CouponAdminService {

    PageResult<CouponTemplateVO> pageTemplate(Long storeId, Long current, Long size,
                                              String keyword, Integer status);

    CouponTemplateVO detailTemplate(Long storeId, Long templateId);

    CouponTemplateVO createTemplate(Long storeId, CouponTemplateSaveDTO dto);

    CouponTemplateVO updateTemplate(Long storeId, Long templateId, CouponTemplateSaveDTO dto);

    /**
     * 状态切换：0-草稿，1-发放中，2-已停用。DB 落库后重置 Redis 库存键。
     */
    CouponTemplateVO updateStatus(Long storeId, Long templateId, Integer status);

    /**
     * 定向发券，返回实际发出的张数。
     */
    int grant(Long storeId, CouponGrantDTO dto);

    PageResult<CouponUseRecordVO> pageUseRecord(Long storeId, Long current, Long size);

    /**
     * 带筛选的核销记录分页。商家端按「员工 + 核销日期」筛选，控制器缺省传当天；
     * 传 null 表示该维度不限制（PC 后台沿用上面的无筛选重载，行为不变）。
     */
    PageResult<CouponUseRecordVO> pageUseRecord(Long storeId, Long current, Long size,
                                                Long operatorUserId, LocalDate verifyDate);

    /**
     * 指定日期内出现过的核销员工，供商家端筛选器渲染。
     */
    List<CouponOperatorVO> listRecordOperators(Long storeId, LocalDate verifyDate);

    /**
     * 券 ROI。templateId 为空时返回本门店全部模板。
     */
    List<CouponRoiVO> roi(Long storeId, Long templateId);

    /**
     * 运维 / 验收补发一条短延迟券过期提醒，验证延迟链路与消费者。
     */
    void resendExpireRemind(Long storeId, Long userCouponId, long delaySeconds);
}
