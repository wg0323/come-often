package com.comeoften.module.payment.controller;

import com.comeoften.common.Result;
import com.comeoften.module.payment.dto.RefundApplyDTO;
import com.comeoften.module.payment.service.RefundService;
import com.comeoften.module.payment.vo.RefundVO;
import com.comeoften.module.store.service.StoreService;
import com.comeoften.security.LoginUserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 顾客端退款申请。仅本人订单可申请；含预定菜的定金单按计划 4.7 由商家后台主动发起。
 */
@Tag(name = "顾客端-退款", description = "申请退款与查询退款单；制作中发起退款进入审核态")
@RestController
@RequestMapping("/api/app/order")
@RequiredArgsConstructor
public class RefundAppController {

    private final RefundService refundService;

    private final StoreService storeService;

    @Operation(summary = "申请退款", description = "PAID 制作前直接退；ACCEPTED/READY 制作中进 REFUNDING 等审核")
    @ApiResponse(responseCode = "200", description = "返回退款单")
    @PostMapping("/{orderNo}/refund")
    public Result<RefundVO> apply(
            @Parameter(description = "订单号", required = true) @PathVariable String orderNo,
            @Valid @RequestBody(required = false) RefundApplyDTO dto) {
        return Result.ok(refundService.applyByMember(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), orderNo, dto));
    }

    @Operation(summary = "查询本单退款单", description = "返回最近一笔退款申请及其状态")
    @ApiResponse(responseCode = "200", description = "返回退款单")
    @GetMapping("/{orderNo}/refund")
    public Result<RefundVO> detail(
            @Parameter(description = "订单号", required = true) @PathVariable String orderNo) {
        return Result.ok(refundService.detailForMember(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), orderNo));
    }
}
