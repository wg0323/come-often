package com.comeoften.integration.pay;

import com.comeoften.integration.pay.model.PayNotifyResult;
import com.comeoften.integration.pay.model.PayOrder;
import com.comeoften.integration.pay.model.PrepayResult;
import com.comeoften.integration.pay.model.RefundOrder;
import com.comeoften.integration.pay.model.RefundResult;

import java.util.Map;

/**
 * 支付网关抽象。计划 3.4：Mock 网关用于本地跑通全流程，微信支付 V3 保留骨架等商户号到手后启用。
 *
 * <p>网关只负责渠道参数与报文解析，订单状态、流水与消息一律由 PaymentService 处理，
 * 避免「渠道实现里散落业务规则」。</p>
 */
public interface PayGateway {

    /**
     * 预下单，返回小程序 wx.requestPayment 所需参数。Mock 网关同步确认收款，V3 网关等异步回调。
     */
    PrepayResult prepay(PayOrder order);

    /**
     * 发起渠道退款。退款单与订单状态由 RefundService 管理，这里只回渠道退款单号。
     */
    RefundResult refund(RefundOrder order);

    /**
     * 解析渠道回调报文，得到商户订单号、渠道交易号与实收金额。
     */
    PayNotifyResult parseNotify(String body, Map<String, String> headers);

    /**
     * 校验渠道回调签名。Mock 模式不接受外部回调，恒返回 false。
     */
    boolean verifySign(String body, Map<String, String> headers);
}
