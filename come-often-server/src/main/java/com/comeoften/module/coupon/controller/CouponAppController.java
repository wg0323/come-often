package com.comeoften.module.coupon.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.coupon.service.UserCouponService;
import com.comeoften.module.coupon.vo.CouponCenterItemVO;
import com.comeoften.module.coupon.vo.CouponCodeVO;
import com.comeoften.module.coupon.vo.UserCouponVO;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 顾客端卡包。会员 ID 取登录态、门店由服务端覆盖；抵扣金额一律由服务端计算。
 */
@Tag(name = "顾客端-卡包", description = "领券、卡包三 Tab 与动态券码")
@RestController
@RequestMapping("/api/app/coupon")
@RequiredArgsConstructor
public class CouponAppController {

    private final UserCouponService userCouponService;

    private final StoreService storeService;

    @Operation(summary = "领券中心", description = "发放中、在可领时间窗内且仍有库存的券；名称含团餐的会在首页团餐券板块展示")
    @ApiResponse(responseCode = "200", description = "返回可领取的券模板列表")
    @GetMapping("/center")
    public Result<List<CouponCenterItemVO>> center() {
        return Result.ok(userCouponService.listCenter(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId()));
    }

    @Operation(summary = "领取优惠券", description = "走 Redis Lua + DB remain 双层库存；领完后返回 30024")
    @ApiResponse(responseCode = "200", description = "返回领取到的券")
    @PostMapping("/{templateId}/receive")
    public Result<UserCouponVO> receive(
            @Parameter(description = "券模板 ID", required = true) @PathVariable Long templateId) {
        return Result.ok(userCouponService.receive(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), templateId));
    }

    @Operation(summary = "我的卡包", description = "tab 取 unused / used / expired，其余值返回全部")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @GetMapping("/page")
    public Result<PageResult<UserCouponVO>> page(
            @Parameter(description = "卡包 Tab：unused / used / expired") @RequestParam(required = false) String tab,
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(userCouponService.pageForMember(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), tab, current, size));
    }

    @Operation(summary = "动态券码", description = "每 30 秒变化一次；已使用返回 30026，已过期返回 30027")
    @ApiResponse(responseCode = "200", description = "返回券码与剩余秒数")
    @GetMapping("/{userCouponId}/code")
    public Result<CouponCodeVO> code(
            @Parameter(description = "用户券 ID", required = true) @PathVariable Long userCouponId) {
        return Result.ok(userCouponService.currentCode(LoginUserHolder.getRequired().getUserId(),
                userCouponId));
    }

}
