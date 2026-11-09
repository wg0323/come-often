package com.comeoften.module.payment.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.payment.dto.RefundApplyDTO;
import com.comeoften.module.payment.dto.RefundAuditDTO;
import com.comeoften.module.payment.service.RefundService;
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
 * PC 后台退款审核。复用既有 order:manage 权限点，不新增菜单与角色配置。
 */
@Tag(name = "PC 后台-退款审核", description = "退款单分页、审核通过/驳回、商家主动发起退款")
@RestController
@RequestMapping("/api/admin/refund")
@RequiredArgsConstructor
public class RefundAdminController {

    private final RefundService refundService;

    @Operation(summary = "退款单分页列表", description = "按门店隔离，可按 0/1/2 状态过滤")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @RequirePerm("order:manage")
    @GetMapping
    public Result<PageResult<RefundVO>> page(
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "状态过滤：0-待审核，1-已退款，2-已拒绝")
            @RequestParam(required = false) Integer status) {
        return Result.ok(refundService.pageForAdmin(LoginUserHolder.getRequired().getStoreId(),
                current, size, status));
    }

    @Operation(summary = "审核退款", description = "通过则 REFUNDING→REFUNDED，驳回则 REFUNDING→ACCEPTED")
    @ApiResponse(responseCode = "200", description = "返回审核后的退款单")
    @RequirePerm("order:manage")
    @PostMapping("/{refundNo}/audit")
    public Result<RefundVO> audit(
            @Parameter(description = "退款单号", required = true) @PathVariable String refundNo,
            @Valid @RequestBody RefundAuditDTO dto) {
        return Result.ok(refundService.audit(LoginUserHolder.getRequired().getStoreId(), refundNo,
                dto, LoginUserHolder.getRequired().getUserId()));
    }

    @Operation(summary = "商家主动发起退款",
            description = "用于超订、临时停业等无法履约场景；含预定菜的定金单只能由此入口退")
    @ApiResponse(responseCode = "200", description = "返回退款单")
    @RequirePerm("order:manage")
    @PostMapping("/order/{orderNo}")
    public Result<RefundVO> applyByAdmin(
            @Parameter(description = "订单号", required = true) @PathVariable String orderNo,
            @Valid @RequestBody(required = false) RefundApplyDTO dto) {
        return Result.ok(refundService.applyByAdmin(LoginUserHolder.getRequired().getStoreId(), orderNo,
                dto, LoginUserHolder.getRequired().getUserId()));
    }
}
