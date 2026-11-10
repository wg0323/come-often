package com.comeoften.module.order.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.order.service.OrderVerifyService;
import com.comeoften.module.order.vo.OrderVerifyRecordVO;
import com.comeoften.module.store.service.StoreService;
import com.comeoften.security.LoginUserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 顾客端核销记录。只回本人本店订单的核销流水，避免越权查他人订单。
 */
@Tag(name = "顾客端-核销记录", description = "订单取餐码 / 套餐核销审计分页与详情")
@RestController
@RequestMapping("/api/app/verify-record")
@RequiredArgsConstructor
public class OrderVerifyAppController {

    private final OrderVerifyService orderVerifyService;

    private final StoreService storeService;

    @Operation(summary = "我的核销记录", description = "按核销时间倒序，仅本人本店订单")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @GetMapping("/page")
    public Result<PageResult<OrderVerifyRecordVO>> page(
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数，最大 50") @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(orderVerifyService.pageForMember(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), current, size));
    }

    @Operation(summary = "核销记录详情", description = "非本人本店记录返回 30090")
    @ApiResponse(responseCode = "200", description = "返回核销记录")
    @GetMapping("/{recordId}")
    public Result<OrderVerifyRecordVO> detail(
            @Parameter(description = "记录 ID", required = true) @PathVariable Long recordId) {
        return Result.ok(orderVerifyService.detailForMember(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), recordId));
    }
}
