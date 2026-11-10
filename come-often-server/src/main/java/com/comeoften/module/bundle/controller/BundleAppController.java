package com.comeoften.module.bundle.controller;

import com.comeoften.common.Result;
import com.comeoften.module.bundle.dto.BundlePurchaseDTO;
import com.comeoften.module.bundle.service.GroupBundleService;
import com.comeoften.module.bundle.vo.BundleVO;
import com.comeoften.module.order.service.OrderService;
import com.comeoften.module.order.vo.OrderVO;
import com.comeoften.module.store.service.StoreService;
import com.comeoften.security.LoginUserHolder;
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
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 顾客端团购套餐。套餐是独立商品，不走购物车 / 菜品报价链路，金额只由服务端按套餐价算。
 */
@Tag(name = "顾客端-团购套餐", description = "套餐列表、详情与下单")
@RestController
@RequestMapping("/api/app/bundle")
@RequiredArgsConstructor
public class BundleAppController {

    private final GroupBundleService groupBundleService;

    private final OrderService orderService;

    private final StoreService storeService;

    @Operation(summary = "在售套餐列表", description = "只返回上架且仍有余量的套餐，含明细菜品")
    @ApiResponse(responseCode = "200", description = "返回套餐列表")
    @GetMapping("/list")
    public Result<List<BundleVO>> list() {
        return Result.ok(groupBundleService.listOnShelf(storeService.defaultStoreId()));
    }

    @Operation(summary = "套餐详情", description = "含套餐明细；下架套餐仍可查看，但不能下单")
    @ApiResponse(responseCode = "200", description = "返回套餐详情")
    @GetMapping("/{bundleId}")
    public Result<BundleVO> detail(
            @Parameter(description = "套餐 ID", required = true) @PathVariable Long bundleId) {
        return Result.ok(groupBundleService.detail(storeService.defaultStoreId(), bundleId, false));
    }

    @Operation(summary = "购买套餐", description = "按套餐价生成一张独立订单；不参与券 / 积分 / 会员折扣 / 余额抵扣；"
            + "库存不足 30079、下架 30078、明细为空 30080")
    @ApiResponse(responseCode = "200", description = "返回待支付订单")
    @PostMapping("/{bundleId}/purchase")
    public Result<OrderVO> purchase(
            @Parameter(description = "套餐 ID", required = true) @PathVariable Long bundleId,
            @Valid @RequestBody BundlePurchaseDTO dto) {
        return Result.ok(orderService.purchaseBundle(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), bundleId, dto));
    }
}
