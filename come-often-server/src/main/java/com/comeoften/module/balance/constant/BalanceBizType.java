package com.comeoften.module.balance.constant;

/**
 * 储值流水业务类型，取值与 {@code balance_record.biz_type} 的注释一致。
 */
public final class BalanceBizType {

    public static final String RECHARGE = "RECHARGE";

    public static final String ORDER = "ORDER";

    public static final String REFUND = "REFUND";

    private BalanceBizType() {
    }
}
