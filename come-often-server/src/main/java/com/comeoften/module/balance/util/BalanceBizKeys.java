package com.comeoften.module.balance.util;

/**
 * 储值流水的业务幂等键。
 *
 * <p>沿用 M6 的 {@code #} 用途后缀口径：同一订单既要写消费又要写回滚 / 退款，
 * {@code uk_biz_idempotent(member_id, biz_type, biz_id)} 才能一条流水一次。
 * 充值单号用 {@code RC} 前缀，与订单号 {@code CO} 前缀区分，同一会员同一天多次充值互不影响。</p>
 */
public final class BalanceBizKeys {

    private static final String SEPARATOR = "#";

    private static final String RECHARGE = "RECHARGE";

    private static final String GIFT = "GIFT";

    private static final String DEDUCT = "DEDUCT";

    private static final String ROLLBACK = "ROLLBACK";

    private static final String REFUND = "REFUND";

    private BalanceBizKeys() {
    }

    /** 充值本金：RECHARGE + {rechargeNo}#RECHARGE。 */
    public static String recharge(String rechargeNo) {
        return rechargeNo + SEPARATOR + RECHARGE;
    }

    /** 充值赠送：RECHARGE + {rechargeNo}#GIFT。 */
    public static String rechargeGift(String rechargeNo) {
        return rechargeNo + SEPARATOR + GIFT;
    }

    /** 下单抵扣：ORDER + {orderNo}#DEDUCT。 */
    public static String orderDeduct(String orderNo) {
        return orderNo + SEPARATOR + DEDUCT;
    }

    /** 取消 / 超时回滚：ORDER + {orderNo}#ROLLBACK，与抵扣行形成正负冲销。 */
    public static String orderRollback(String orderNo) {
        return orderNo + SEPARATOR + ROLLBACK;
    }

    /** 整单退款退回余额：REFUND + {orderNo}#REFUND。 */
    public static String orderRefund(String orderNo) {
        return orderNo + SEPARATOR + REFUND;
    }
}
