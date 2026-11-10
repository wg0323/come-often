package com.comeoften.module.balance.constant;

/**
 * 储值流水方向，取值与 {@code balance_record.change_type} 的注释一致。
 */
public final class BalanceChangeType {

    public static final String RECHARGE = "RECHARGE";

    public static final String GIFT = "GIFT";

    public static final String CONSUME = "CONSUME";

    public static final String REFUND = "REFUND";

    public static final String ROLLBACK = "ROLLBACK";

    private BalanceChangeType() {
    }
}
