package com.comeoften.module.points.constant;

/**
 * 积分业务类型，取值与 points_record.biz_type 的注释一致。
 *
 * <p>业务类型 + biz_id 组成 points_record 的唯一幂等键 uk_points_idempotent，
 * 同一业务重复投递时只会落一条流水。</p>
 */
public final class PointsBizType {

    public static final String ORDER = "ORDER";

    public static final String SIGN_IN = "SIGN_IN";

    public static final String REVIEW = "REVIEW";

    public static final String SHARE = "SHARE";

    public static final String PROFILE = "PROFILE";

    /**
     * 积分兑换菜品。与 ORDER 的积分抵现（REDEEM）刻意分开：
     * 抵现是「少付现金」，兑换是「整单用积分结算」，统计与回滚口径都不同。
     */
    public static final String EXCHANGE = "EXCHANGE";

    private PointsBizType() {
    }
}
