package com.comeoften.module.coupon.service;

import com.comeoften.module.coupon.dto.CouponGrantRuleSaveDTO;
import com.comeoften.module.coupon.vo.CouponGrantRuleVO;

import java.util.List;

/**
 * 券发放规则。四条规则分别由三个入口驱动：
 * <ul>
 *   <li>{@code NEW_MEMBER}：首单支付成功事件；</li>
 *   <li>{@code REPEAT_PURCHASE}：订单完成事件（累计支付单数达标）；</li>
 *   <li>{@code BIRTHDAY} / {@code SLEEPING}：每日 {@code coupon-rule} 任务，也可按类型手动补跑。</li>
 * </ul>
 * 所有规则的发放都带 {@code user_coupon.grant_biz_id} 幂等键，重复执行不会重复发券。
 */
public interface CouponRuleService {

    List<CouponGrantRuleVO> list(Long storeId);

    CouponGrantRuleVO save(Long storeId, CouponGrantRuleSaveDTO dto);

    CouponGrantRuleVO setEnabled(Long storeId, Long ruleId, Boolean enabled);

    /**
     * 按类型手动执行规则（BIRTHDAY / SLEEPING 有效，另外两类事件驱动返回 0），返回发券张数。
     */
    int run(Long storeId, String ruleType);

    /**
     * 首单支付成功：发放启用中的新客券。
     */
    void onFirstPaidOrder(Long memberId, Long storeId, Long orderId);

    /**
     * 订单完成：累计支付单数达标时发放复购券。
     */
    void onOrderCompleted(Long memberId, Long storeId, String orderNo);
}
