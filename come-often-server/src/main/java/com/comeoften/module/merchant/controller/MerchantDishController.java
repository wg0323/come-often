package com.comeoften.module.merchant.controller;

import com.comeoften.common.Result;
import com.comeoften.module.merchant.dto.SoldOutToggleDTO;
import com.comeoften.module.merchant.service.MerchantSoldOutService;
import com.comeoften.module.merchant.vo.SoldOutCategoryVO;
import com.comeoften.security.LoginUserHolder;
import com.comeoften.security.RequirePerm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 商家端菜品售罄。走 M2/M3 的库存服务，售罄后顾客端下单立即被拒（DB 是事实源）。
 */
@Tag(name = "商家端-菜品售罄", description = "分类分组的一键售罄 / 开售，库存规则复用 PC 后台")
@RestController
@RequestMapping("/api/merchant/dish")
@RequiredArgsConstructor
public class MerchantDishController {

    private final MerchantSoldOutService merchantSoldOutService;

    @Operation(summary = "售罄列表", description = "按分类分组，只返回本店上架菜品与当日库存")
    @ApiResponse(responseCode = "200", description = "返回分类分组")
    @RequirePerm("dish:soldout")
    @GetMapping("/soldout")
    public Result<List<SoldOutCategoryVO>> list() {
        return Result.ok(merchantSoldOutService.list(LoginUserHolder.getRequired().getStoreId()));
    }

    @Operation(summary = "一键售罄 / 开售", description = "售罄只置余量 0；开售按当前总量或默认 50 份补齐")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @RequirePerm("dish:soldout")
    @PutMapping("/soldout")
    public Result<Void> toggle(@Valid @RequestBody SoldOutToggleDTO dto) {
        merchantSoldOutService.toggle(LoginUserHolder.getRequired().getStoreId(), dto.getDishId(),
                Boolean.TRUE.equals(dto.getSoldOut()));
        return Result.ok();
    }
}
