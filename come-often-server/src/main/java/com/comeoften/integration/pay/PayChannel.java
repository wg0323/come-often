package com.comeoften.integration.pay;

/**
 * payment_record.pay_channel 的取值。BALANCE 是 M14 B1 起预留的储值余额支付渠道，
 * 真实写入由 B3 组合支付实现；本批只把取值收口到常量，避免各处散落字符串。
 */
public final class PayChannel {

    public static final String MOCK = "MOCK";

    public static final String WXPAY_V3 = "WXPAY_V3";

    public static final String BALANCE = "BALANCE";

    private PayChannel() {
    }
}
