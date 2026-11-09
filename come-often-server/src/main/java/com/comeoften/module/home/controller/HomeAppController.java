package com.comeoften.module.home.controller;

import com.comeoften.common.Result;
import com.comeoften.module.home.service.HomeService;
import com.comeoften.module.home.vo.HomeSummaryVO;
import com.comeoften.module.store.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 顾客端首页。
 */
@Tag(name = "顾客端-首页", description = "门店条、轮播、公告、分层入口与招牌菜一次返回")
@RestController
@RequestMapping("/api/app/home")
@RequiredArgsConstructor
public class HomeAppController {

    private final HomeService homeService;

    private final StoreService storeService;

    @Operation(summary = "首页汇总", description = "轮播最多 3 张，按权重降序；招牌菜取 SIGNATURE 标签")
    @ApiResponse(responseCode = "200", description = "返回首页汇总")
    @GetMapping("/summary")
    public Result<HomeSummaryVO> summary() {
        return Result.ok(homeService.summary(storeService.defaultStoreId()));
    }
}
