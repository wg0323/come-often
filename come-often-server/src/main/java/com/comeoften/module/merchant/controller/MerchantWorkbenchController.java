package com.comeoften.module.merchant.controller;

import com.comeoften.common.Result;
import com.comeoften.module.merchant.service.MerchantWorkbenchService;
import com.comeoften.module.merchant.vo.WorkbenchSummaryVO;
import com.comeoften.security.LoginUserHolder;
import com.comeoften.security.RequirePerm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家端工作台概览。门店取当前登录员工的归属门店（sys_store_user）。
 */
@Tag(name = "商家端-工作台", description = "待接单 / 制作中 / 待取餐三个数字、今日预约数与今日营收")
@RestController
@RequestMapping("/api/merchant/workbench")
@RequiredArgsConstructor
public class MerchantWorkbenchController {

    private final MerchantWorkbenchService merchantWorkbenchService;

    @Operation(summary = "工作台概览", description = "订单计数与订单看板同源；营收取当日已支付的实付金额汇总")
    @ApiResponse(responseCode = "200", description = "返回工作台概览")
    @RequirePerm("workbench:view")
    @GetMapping("/summary")
    public Result<WorkbenchSummaryVO> summary() {
        return Result.ok(merchantWorkbenchService.summary(LoginUserHolder.getRequired().getStoreId()));
    }
}
