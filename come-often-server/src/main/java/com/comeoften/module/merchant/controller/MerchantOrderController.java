package com.comeoften.module.merchant.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.merchant.service.MerchantOrderService;
import com.comeoften.module.order.dto.OrderTransitionDTO;
import com.comeoften.module.order.vo.OrderSummaryVO;
import com.comeoften.module.order.vo.OrderVO;
import com.comeoften.module.payment.dto.RefundApplyDTO;
import com.comeoften.module.payment.vo.RefundVO;
import com.comeoften.security.LoginUserHolder;
import com.comeoften.security.RequirePerm;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 商家端订单看板。四 Tab 列表、详情、接单 / 出餐 / 确认取餐与发起退单。
 */
@Tag(name = "商家端-订单看板", description = "订单分页、详情、接单出餐确认取餐与发起退单")
@RestController
@RequestMapping("/api/merchant/order")
@RequiredArgsConstructor
public class MerchantOrderController {

    private final MerchantOrderService merchantOrderService;

    @Operation(summary = "订单分页", description = "按状态过滤：PAID 待接单 / ACCEPTED 制作中 / READY 待取餐 / COMPLETED 已完成")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @RequirePerm("order:accept")
    @GetMapping("/page")
    public Result<PageResult<OrderSummaryVO>> page(
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数，上限 50") @RequestParam(defaultValue = "20") Long size,
            @Parameter(description = "状态过滤") @RequestParam(required = false) String status) {
        return Result.ok(merchantOrderService.page(LoginUserHolder.getRequired().getStoreId(),
                current, size, status));
    }

    @Operation(summary = "订单详情", description = "含取餐码、备注、金额快照、菜品明细与状态日志")
    @ApiResponse(responseCode = "200", description = "返回订单详情")
    @RequirePerm("order:accept")
    @GetMapping("/{orderNo}")
    public Result<OrderVO> detail(
            @Parameter(description = "订单号", required = true) @PathVariable String orderNo) {
        return Result.ok(merchantOrderService.detail(LoginUserHolder.getRequired().getStoreId(), orderNo));
    }

    @Operation(summary = "接单 / 出餐 / 确认取餐", description = "目标状态只允许 ACCEPTED / READY / COMPLETED，非法迁移返回 30002")
    @ApiResponse(responseCode = "200", description = "返回推进后的订单详情")
    @RequirePerm("order:accept")
    @PostMapping("/{orderNo}/transition")
    public Result<OrderVO> transition(
            @Parameter(description = "订单号", required = true) @PathVariable String orderNo,
            @Valid @RequestBody OrderTransitionDTO dto) {
        return Result.ok(merchantOrderService.advance(LoginUserHolder.getRequired().getStoreId(),
                orderNo, dto.getToStatus(), dto.getRemark(), LoginUserHolder.getRequired().getUserId()));
    }

    @Operation(summary = "发起退单", description = "制作前自动通过，制作中 / 待取餐进退款审核；金额由服务端按已收流水计算")
    @ApiResponse(responseCode = "200", description = "返回退款单")
    @RequirePerm("order:accept")
    @PostMapping("/{orderNo}/refund")
    public Result<RefundVO> refund(
            @Parameter(description = "订单号", required = true) @PathVariable String orderNo,
            @Valid @RequestBody(required = false) RefundApplyDTO dto) {
        return Result.ok(merchantOrderService.refund(LoginUserHolder.getRequired().getStoreId(), orderNo,
                dto == null ? null : dto.getReason(), LoginUserHolder.getRequired().getUserId()));
    }
}
