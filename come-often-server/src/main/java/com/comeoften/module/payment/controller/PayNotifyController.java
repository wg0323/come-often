package com.comeoften.module.payment.controller;

import com.comeoften.module.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 渠道支付回调。M4 只写骨架：WxPayV3Gateway 未启用，验签恒失败并返回 30013；
 * 商户号到手后补平台证书验签、APIv3 解密与失败重试状态码，业务侧 PaymentService 不用改。
 */
@Tag(name = "支付回调", description = "渠道异步通知入口；Mock 网关下拒绝外部回调")
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayNotifyController {

    private final PaymentService paymentService;

    @Operation(summary = "渠道支付成功回调", description = "先验签再解析，最后走与 Mock 相同的入账链路")
    @ApiResponse(responseCode = "200", description = "渠道要求的 SUCCESS 应答")
    @PostMapping("/notify")
    public Map<String, String> notify(
            @Parameter(description = "渠道回调原始报文", required = true) @RequestBody String body,
            @Parameter(description = "渠道签名相关请求头") @RequestHeader Map<String, String> headers) {
        paymentService.handleNotify(body, headers);
        Map<String, String> ack = new LinkedHashMap<>(2);
        ack.put("code", "SUCCESS");
        ack.put("message", "成功");
        return ack;
    }
}
