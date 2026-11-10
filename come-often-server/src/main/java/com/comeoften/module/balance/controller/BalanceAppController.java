package com.comeoften.module.balance.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.balance.dto.BalanceRechargeDTO;
import com.comeoften.module.balance.service.BalanceService;
import com.comeoften.module.balance.vo.BalanceAccountVO;
import com.comeoften.module.balance.vo.BalanceRechargeVO;
import com.comeoften.module.balance.vo.BalanceRecordVO;
import com.comeoften.security.LoginUserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 顾客端储值。会员 ID 取登录态，充值金额由服务端校验区间与小数位，赠送额按档位算。
 */
@Tag(name = "顾客端-储值", description = "储值账户、充值入口与余额流水")
@RestController
@RequestMapping("/api/app/balance")
@RequiredArgsConstructor
public class BalanceAppController {

    private final BalanceService balanceService;

    @Operation(summary = "储值账户", description = "余额、累计充值 / 赠送 / 消费与充值赠送档位；新会员返回零值账户")
    @ApiResponse(responseCode = "200", description = "返回账户概览")
    @GetMapping("/account")
    public Result<BalanceAccountVO> account() {
        return Result.ok(balanceService.account(LoginUserHolder.getRequired().getUserId()));
    }

    @Operation(summary = "储值充值",
            description = "当前为 Mock 渠道直接入账（payChannel=MOCK）；金额超出 30065 区间或小数位返回 30065，"
                    + "赠送额按 comeoften.balance.recharge-tiers 的最高命中档位计算")
    @ApiResponse(responseCode = "200", description = "返回充值单号、本金、赠送与充值后余额")
    @PostMapping("/recharge")
    public Result<BalanceRechargeVO> recharge(@Valid @RequestBody BalanceRechargeDTO dto) {
        return Result.ok(balanceService.recharge(LoginUserHolder.getRequired().getUserId(), dto));
    }

    @Operation(summary = "储值流水", description = "按发生时间倒序分页")
    @ApiResponse(responseCode = "200", description = "返回分页流水")
    @GetMapping("/record/page")
    public Result<PageResult<BalanceRecordVO>> recordPage(
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数，最大 50") @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(balanceService.recordPage(LoginUserHolder.getRequired().getUserId(),
                current, size));
    }
}
