package com.comeoften.module.merchant.controller;

import com.comeoften.common.Result;
import com.comeoften.module.merchant.dto.ScanResolveDTO;
import com.comeoften.module.merchant.service.MerchantScanService;
import com.comeoften.module.merchant.vo.ScanResolveVO;
import com.comeoften.security.LoginUserHolder;
import com.comeoften.security.RequirePerm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 商家端统一扫码。只有一个入口：前端不做码型判断，后端按码串分流并直接执行，
 * 失败以 success=false + failReason 返回，避免同一份核销规则出现两套判据。
 */
@Tag(name = "商家端-扫码核销", description = "券码 / 预约签到码 / 取餐码统一扫码，后端自动分流")
@RestController
@RequestMapping("/api/merchant/scan")
@RequiredArgsConstructor
public class MerchantScanController {

    private final MerchantScanService merchantScanService;

    @Operation(summary = "统一扫码核销",
            description = "C 前缀券码核销、R 前缀签到码签到、A001~D999 取餐码确认取餐；失败返回 success=false 与失败原因")
    @ApiResponse(responseCode = "200", description = "返回统一扫码结果")
    @RequirePerm("scan:use")
    @PostMapping("/resolve")
    public Result<ScanResolveVO> resolve(@Valid @RequestBody ScanResolveDTO dto) {
        return Result.ok(merchantScanService.resolve(LoginUserHolder.getRequired().getStoreId(),
                LoginUserHolder.getRequired().getUserId(), dto.getCode()));
    }
}
