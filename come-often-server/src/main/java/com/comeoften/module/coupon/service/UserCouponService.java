package com.comeoften.module.coupon.service;

import com.comeoften.common.PageResult;
import com.comeoften.module.coupon.vo.CouponCenterItemVO;
import com.comeoften.module.coupon.vo.CouponCodeVO;
import com.comeoften.module.coupon.vo.CouponSelectionVO;
import com.comeoften.module.coupon.vo.UserCouponVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 会员侧持券：领券、卡包三 Tab、动态券码、下单可用券与下单锁定 / 退回。
 * 抵扣金额一律由本服务用 CouponDiscountCalculator 计算，页面不做业务计算。
 */
public interface UserCouponService {

    /**
     * 领券中心：发放中、在可领时间窗内且仍有库存的券模板；名称含「团餐」的标记为团餐券。
     */
    List<CouponCenterItemVO> listCenter(Long memberId, Long storeId);

    /**
     * 领券。库存不足抛 30024；模板未发放或已停用由 CouponGrantService 抛 30023。
     */
    UserCouponVO receive(Long memberId, Long storeId, Long templateId);

    /**
     * 卡包分页。tab 取 unused / used / expired，其余值视为全部。
     */
    PageResult<UserCouponVO> pageForMember(Long memberId, Long storeId, String tab, Long current, Long size);

    /**
     * 动态券码。已使用抛 30026，已过期抛 30027 并顺手置状态。
     */
    CouponCodeVO currentCode(Long memberId, Long userCouponId);

    /**
     * 当前订单可用的券。usable=false 的券一并返回并给出原因，便于页面提示。
     *
     * @param dishAmounts 菜品 ID → 行金额小计，供单品券判定可用性与抵扣额
     */
    List<UserCouponVO> listUsable(Long memberId, Long storeId, BigDecimal orderAmount,
                                  Map<Long, BigDecimal> dishAmounts);

    /**
     * 校验并计算某张券在指定订单金额下的抵扣额，不写库。
     */
    CouponSelectionVO resolveForOrder(Long memberId, Long storeId, Long userCouponId,
                                      BigDecimal orderAmount, Map<Long, BigDecimal> dishAmounts);

    /**
     * 下单锁定：状态 0→1 并绑定订单。并发下只有一个能成功，失败抛 30026。
     */
    void lockForOrder(Long memberId, Long userCouponId, Long orderId);

    /**
     * 订单取消退回：只退回被该订单锁定的券。
     */
    void releaseForOrder(Long userCouponId, Long orderId);
}
