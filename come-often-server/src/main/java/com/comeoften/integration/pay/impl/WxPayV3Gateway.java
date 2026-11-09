package com.comeoften.integration.pay.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.config.PayProperties;
import com.comeoften.integration.pay.PayGateway;
import com.comeoften.integration.pay.model.PayNotifyResult;
import com.comeoften.integration.pay.model.PayOrder;
import com.comeoften.integration.pay.model.PrepayResult;
import com.comeoften.integration.pay.model.RefundOrder;
import com.comeoften.integration.pay.model.RefundResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 微信支付 V3 骨架。计划 3.4：M4 只保留实现与配置项，不启用、不引入 weixin-java-pay。
 *
 * <p>商户号到手后需要补齐：JSAPI 下单、回调验签（平台证书 / 公钥）、APIv3 密钥解密、
 * 退款申请与退款结果通知、每日对账单下载与差异核对。业务侧只依赖 PayGateway，不改订单与退款代码。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "comeoften.pay.gateway", havingValue = "wxpay")
public class WxPayV3Gateway implements PayGateway {

    private static final String NOT_ENABLED = "微信支付 V3 网关尚未启用，需先配置商户号、证书与 APIv3 密钥";

    private final PayProperties payProperties;

    @Override
    public PrepayResult prepay(PayOrder order) {
        log.warn("WxPayV3 骨架未实现 JSAPI 下单: mchId={}", payProperties.getWxpay().getMchId());
        throw new BizException(ErrorCode.PAY_GATEWAY_NOT_ENABLED, NOT_ENABLED);
    }

    @Override
    public RefundResult refund(RefundOrder order) {
        log.warn("WxPayV3 骨架未实现退款申请: refundNo={}", order.getRefundNo());
        throw new BizException(ErrorCode.PAY_GATEWAY_NOT_ENABLED, NOT_ENABLED);
    }

    @Override
    public PayNotifyResult parseNotify(String body, Map<String, String> headers) {
        throw new BizException(ErrorCode.PAY_GATEWAY_NOT_ENABLED, NOT_ENABLED);
    }

    @Override
    public boolean verifySign(String body, Map<String, String> headers) {
        return false;
    }
}
