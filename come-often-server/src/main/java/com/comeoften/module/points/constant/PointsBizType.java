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

    private PointsBizType() {
    }
}
