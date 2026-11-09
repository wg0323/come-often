package com.comeoften.module.coupon.service;

import com.comeoften.module.coupon.constant.VerifyChannel;
import com.comeoften.module.coupon.dto.CouponVerifyDTO;
import com.comeoften.module.coupon.vo.CouponVerifyVO;

/**
 * 券核销。全项目只有这一份业务规则：PC 后台输码 / 扫码枪走 ADMIN_INPUT，
 * M9 商家端 wx.scanCode 走 MERCHANT_SCAN，两者共用本接口，不复制规则。
 */
public interface CouponVerifyService {

    /**
     * 核销。已使用抛 30026，已过期抛 30027，码不在时间窗内抛 30028，非本店抛 30030。
     *
     * @param storeId        核销门店，取登录员工归属门店
     * @param operatorUserId 核销员工，落 coupon_use_record.operator_user_id 供审计
     */
    CouponVerifyVO verify(Long storeId, Long operatorUserId, CouponVerifyDTO dto, VerifyChannel channel);
}
