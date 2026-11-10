package com.comeoften.module.points.util;

import com.comeoften.module.points.constant.PointsBizType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 积分流水的业务幂等键构造。
 *
 * <p>uk_points_idempotent(member_id, biz_type, biz_id) 是唯一索引，同一订单既要发「消费得分」
 * 又要发「抵现冻结 / 实扣 / 回滚」，因此按用途加后缀区分，既保住一条流水一次的幂等语义，
 * 又不需要给 M0 表结构补列。</p>
 */
public final class PointsBizKeys {

    private static final String SEPARATOR = "#";

    private static final String EARN = "EARN";

    private static final String REDEEM = "REDEEM";

    private static final String ROLLBACK = "ROLLBACK";

    private static final String EXPIRE = "EXPIRE";

    private static final String EXCHANGE = "EXCHANGE";

    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private PointsBizKeys() {
    }

    /**
     * 订单完成发放积分：ORDER + {orderNo}#EARN。
     */
    public static String orderEarn(String orderNo) {
        return orderNo + SEPARATOR + EARN;
    }

    /**
     * 积分抵现冻结与实扣共用同一行：ORDER + {orderNo}#REDEEM。
     */
    public static String orderRedeem(String orderNo) {
        return orderNo + SEPARATOR + REDEEM;
    }

    /**
     * 取消或超时回滚：ORDER + {orderNo}#ROLLBACK，与冻结行同时存在，形成正负冲销。
     */
    public static String orderRollback(String orderNo) {
        return orderNo + SEPARATOR + ROLLBACK;
    }

    /**
     * 积分兑换菜品：EXCHANGE + {orderNo}#EXCHANGE。
     */
    public static String exchange(String orderNo) {
        return orderNo + SEPARATOR + EXCHANGE;
    }

    /**
     * 兑换单取消退回：EXCHANGE + {orderNo}#EXCHANGE_ROLLBACK，与兑换行正负相抵。
     */
    public static String exchangeRollback(String orderNo) {
        return orderNo + SEPARATOR + EXCHANGE + "_ROLLBACK";
    }

    /**
     * 过期扣减：沿用原流水的 biz_type，biz_id = {原流水ID}#EXPIRE，一条积分只能过期一次。
     */
    public static String expire(Long recordId) {
        return recordId + SEPARATOR + EXPIRE;
    }

    /**
     * 签到与分享按天各一次：biz_type=SIGN_IN / SHARE，biz_id=yyyyMMdd。
     */
    public static String byDay(LocalDate date) {
        return DAY_FORMATTER.format(date);
    }

    /**
     * 完善资料终身一次：PROFILE + PROFILE。
     */
    public static String profile() {
        return PointsBizType.PROFILE;
    }

    /**
     * 评价按评价 ID 一次：REVIEW + {reviewId}。
     */
    public static String review(Long reviewId) {
        return String.valueOf(reviewId);
    }
}
