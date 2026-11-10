package com.comeoften.module.coupon.service;

import java.util.List;

/**
 * 发券。后台定向发放与新人券自动发放共用同一条链路：
 * Redis Lua 预扣 → DB remain 兜底 → 写 user_coupon → 登记券过期提醒延迟消息。
 */
public interface CouponGrantService {

    /**
     * 批量发券。库存不足时只发到发完为止，返回实际发出的张数。
     *
     * @param source 发放来源，仅用于日志与追溯（如 ADMIN / NEWCOMER:{orderId}）
     */
    int grant(Long storeId, Long templateId, List<Long> memberIds, String source);

    /**
     * 规则发券：除库存链路外，额外用 {@code user_coupon.grant_biz_id} 做业务幂等
     * （如 {@code {memberId}#RULE#BIRTHDAY#2026}），同一幂等键只发一张。
     */
    int grantWithBizId(Long storeId, Long templateId, Long memberId, String source, String grantBizId);

    /**
     * 支付成功后按首单判定发新人券。非首单、模板未配置或已发过时返回 0。
     * 幂等：同一会员同一模板只发一次，并叠加消息级 Redis 幂等键。
     */
    int grantNewcomerIfFirstPaidOrder(Long memberId, Long storeId, Long orderId);

    /**
     * 运维 / 验收补发一条短延迟券过期提醒，走与真实提醒完全相同的延迟链路。
     */
    void resendExpireRemind(Long userCouponId, long delaySeconds);
}
