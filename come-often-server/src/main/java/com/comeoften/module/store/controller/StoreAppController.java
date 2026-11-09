package com.comeoften.module.store.controller;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.Result;
import com.comeoften.module.store.service.StoreService;
import com.comeoften.module.store.vo.StoreVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 顾客端门店信息。详情只开放当前门店，避免按 ID 枚举其他门店配置。
 */
@Tag(name = "顾客端-门店", description = "门店列表与详情，含营业时间与桌台")
@RestController
@RequestMapping("/api/app/store")
@RequiredArgsConstructor
public class StoreAppController {

    private final StoreService storeService;

    @Operation(summary = "门店列表", description = "一期只有 1 家门店，结构按多店预留")
    @ApiResponse(responseCode = "200", description = "返回门店列表")
    @GetMapping("/list")
    public Result<List<StoreVO>> list() {
        return Result.ok(storeService.list());
    }

    @Operation(summary = "门店详情", description = "只能查当前门店；传入其他门店 ID 返回 10001")
    @ApiResponse(responseCode = "200", description = "返回门店详情")
    @GetMapping("/{storeId}")
    public Result<StoreVO> detail(
            @Parameter(description = "门店 ID", required = true) @PathVariable Long storeId) {
        Long currentStoreId = storeService.defaultStoreId();
        if (!currentStoreId.equals(storeId)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "门店不存在");
        }
        return Result.ok(storeService.detail(currentStoreId));
    }
}
