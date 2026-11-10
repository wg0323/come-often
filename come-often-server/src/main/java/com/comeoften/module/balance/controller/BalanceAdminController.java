package com.comeoften.module.balance.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.balance.service.BalanceAdminService;
import com.comeoften.module.balance.vo.BalanceMemberVO;
import com.comeoften.module.balance.vo.BalanceRecordVO;
import com.comeoften.security.RequirePerm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * PC 后台储值管理。权限点是 B1 预登记的 balance:manage（菜单 110）。
 */
@Tag(name = "PC 后台-储值管理", description = "储值会员与储值流水查询")
@RestController
@RequestMapping("/api/admin/balance")
@RequiredArgsConstructor
public class BalanceAdminController {

    private final BalanceAdminService balanceAdminService;

    @Operation(summary = "储值会员分页", description = "keyword 命中昵称或手机号，手机号脱敏返回")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @RequirePerm("balance:manage")
    @GetMapping("/member/page")
    public Result<PageResult<BalanceMemberVO>> memberPage(
            @Parameter(description = "昵称或手机号关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(balanceAdminService.memberPage(current, size, keyword));
    }

    @Operation(summary = "储值流水分页", description = "memberId 为空时查全部会员流水")
    @ApiResponse(responseCode = "200", description = "返回分页流水")
    @RequirePerm("balance:manage")
    @GetMapping("/record/page")
    public Result<PageResult<BalanceRecordVO>> recordPage(
            @Parameter(description = "会员 ID，可空") @RequestParam(required = false) Long memberId,
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(balanceAdminService.recordPage(memberId, current, size));
    }
}
