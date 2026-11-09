package com.comeoften.module.merchant.controller;

import com.comeoften.common.Result;
import com.comeoften.module.merchant.service.MerchantNotifyService;
import com.comeoften.module.merchant.vo.MerchantNotifyVO;
import com.comeoften.security.LoginUserHolder;
import com.comeoften.security.RequirePerm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家端新订单提醒埋点。支付成功事件已经由 order.notify 消费者自动触发一次，
 * 这个入口供前台检测到新单后补发或验收使用；模板未配置时降级落 message_record + 日志。
 */
@Tag(name = "商家端-新订单提醒", description = "订阅消息埋点；模板缺失时降级落库，不阻塞主流程")
@RestController
@RequestMapping("/api/merchant/notify")
@RequiredArgsConstructor
public class MerchantNotifyController {

    private final MerchantNotifyService merchantNotifyService;

    @Operation(summary = "补发新订单提醒", description = "订单需处于待接单 / 制作中 / 待取餐状态；返回实际通道与模板配置情况")
    @ApiResponse(responseCode = "200", description = "返回提醒结果")
    @RequirePerm("order:accept")
    @PostMapping("/new-order")
    public Result<MerchantNotifyVO> remindNewOrder(
            @Parameter(description = "订单号", required = true) @RequestParam String orderNo) {
        return Result.ok(merchantNotifyService.remindNewOrder(LoginUserHolder.getRequired().getStoreId(),
                orderNo, LoginUserHolder.getRequired().getUserId()));
    }
}
