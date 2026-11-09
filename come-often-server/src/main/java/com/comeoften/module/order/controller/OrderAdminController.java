package com.comeoften.module.order.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.order.constant.OperatorType;
import com.comeoften.module.order.dto.OrderTransitionDTO;
import com.comeoften.module.order.service.OrderService;
import com.comeoften.module.order.vo.OrderSummaryVO;
import com.comeoften.module.order.vo.OrderVO;
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
 * PC 后台订单看板。所有接口需要 order:manage 权限点，门店取当前登录员工的归属门店。
 */
@Tag(name = "PC 后台-订单看板", description = "订单分页、详情、接单/出餐/确认取餐与超时消息补发")
@RestController
@RequestMapping("/api/admin/order")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderService orderService;

    @Operation(summary = "订单分页列表", description = "按状态与订单号过滤，供看板四 Tab 使用")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @RequirePerm("order:manage")
    @GetMapping("/page")
    public Result<PageResult<OrderSummaryVO>> page(
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "状态过滤") @RequestParam(required = false) String status,
            @Parameter(description = "订单号关键词") @RequestParam(required = false) String keyword) {
        return Result.ok(orderService.pageForAdmin(LoginUserHolder.getRequired().getStoreId(),
                current, size, status, keyword));
    }

    @Operation(summary = "订单详情", description = "含金额快照、明细与状态日志")
    @ApiResponse(responseCode = "200", description = "返回订单详情")
    @RequirePerm("order:manage")
    @GetMapping("/{orderNo}")
    public Result<OrderVO> detail(
            @Parameter(description = "订单号", required = true) @PathVariable String orderNo) {
        return Result.ok(orderService.detailForAdmin(LoginUserHolder.getRequired().getStoreId(), orderNo));
    }

    @Operation(summary = "推进订单状态", description = "接单 / 出餐 / 确认取餐；非法迁移返回 30002")
    @ApiResponse(responseCode = "200", description = "返回推进后的订单详情")
    @RequirePerm("order:manage")
    @PostMapping("/{orderNo}/transition")
    public Result<OrderVO> transition(
            @Parameter(description = "订单号", required = true) @PathVariable String orderNo,
            @Valid @RequestBody OrderTransitionDTO dto) {
        return Result.ok(orderService.transition(LoginUserHolder.getRequired().getStoreId(), orderNo,
                dto.getToStatus(), dto.getRemark(), OperatorType.ADMIN,
                LoginUserHolder.getRequired().getUserId()));
    }

    @Operation(summary = "补发订单超时消息", description = "运维 / 验收用：发一条短延迟消息验证到点取消与库存回补")
    @ApiResponse(responseCode = "200", description = "补发成功")
    @RequirePerm("order:manage")
    @PostMapping("/{orderNo}/resend-timeout")
    public Result<Void> resendTimeout(
            @Parameter(description = "订单号", required = true) @PathVariable String orderNo,
            @Parameter(description = "延迟秒数，1~3600") @RequestParam(defaultValue = "3") Long delaySeconds) {
        orderService.resendTimeoutMessage(LoginUserHolder.getRequired().getStoreId(),
                orderNo, delaySeconds);
        return Result.ok();
    }
}
