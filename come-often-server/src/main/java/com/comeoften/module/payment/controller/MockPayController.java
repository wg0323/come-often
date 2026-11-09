package com.comeoften.module.payment.controller;

import com.comeoften.common.Result;
import com.comeoften.module.order.vo.OrderVO;
import com.comeoften.module.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 本地 Mock 支付回调。计划 3.4：手动触发支付成功，用于跑通全流程与验证回调重放幂等。
 *
 * <p>仅在 dev profile + payment.gateway=mock 时装配；prod profile 或切到 wxpay 后本端点自动消失，
 * 避免新 profile 忘记设置网关属性时留下无鉴权刷单入口。匿名访问是刻意的：模拟渠道通知，
 * 与微信回调一样不能带登录态。</p>
 */
@Tag(name = "Mock 支付", description = "本地无商户号时手动触发支付成功回调；仅 dev + Mock 网关下可用")
@RestController
@RequestMapping("/api/mock/pay")
@RequiredArgsConstructor
@Profile("dev")
@ConditionalOnProperty(name = "comeoften.pay.gateway", havingValue = "mock", matchIfMissing = true)
public class MockPayController {

    private final PaymentService paymentService;

    @Operation(summary = "手动触发支付成功回调",
            description = "金额由服务端订单快照给出；重复调用按 out_trade_no 幂等，只产生一条支付流水")
    @ApiResponse(responseCode = "200", description = "返回入账后的订单详情；重复回调不会二次改状态")
    @PostMapping("/success")
    public Result<OrderVO> success(
            @Parameter(description = "订单号", required = true) @RequestParam String orderNo) {
        return Result.ok(paymentService.simulateSuccess(orderNo));
    }
}
