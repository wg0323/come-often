package com.comeoften.module.merchant.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.coupon.service.CouponAdminService;
import com.comeoften.module.coupon.vo.CouponOperatorVO;
import com.comeoften.module.coupon.vo.CouponUseRecordVO;
import com.comeoften.security.LoginUserHolder;
import com.comeoften.security.RequirePerm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 商家端核销记录。与 PC 后台共用同一张 coupon_use_record 与同一套查询，
 * 只按「员工 + 核销日期」加筛选；撤销核销刻意不提供（纠错统一回 PC 后台走审批）。
 */
@Tag(name = "商家端-核销记录", description = "今日核销流水，按员工与日期筛选，不提供撤销")
@RestController
@RequestMapping("/api/merchant/coupon")
@RequiredArgsConstructor
public class MerchantCouponController {

    private final CouponAdminService couponAdminService;

    @Operation(summary = "核销记录分页", description = "默认查当日；每页条数上限沿用 PC 后台的 100")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @RequirePerm("coupon:record")
    @GetMapping("/record/page")
    public Result<PageResult<CouponUseRecordVO>> page(
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数，上限 100") @RequestParam(defaultValue = "20") Long size,
            @Parameter(description = "核销员工 ID，可空") @RequestParam(required = false) Long operatorUserId,
            @Parameter(description = "核销日期 yyyy-MM-dd，缺省为当天")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate verifyDate = date == null ? LocalDate.now() : date;
        return Result.ok(couponAdminService.pageUseRecord(LoginUserHolder.getRequired().getStoreId(),
                current, size, operatorUserId, verifyDate));
    }

    @Operation(summary = "当日核销员工", description = "供商家端员工筛选器渲染，只返回当日有核销记录的员工")
    @ApiResponse(responseCode = "200", description = "返回员工列表")
    @RequirePerm("coupon:record")
    @GetMapping("/record/operators")
    public Result<List<CouponOperatorVO>> operators(
            @Parameter(description = "核销日期 yyyy-MM-dd，缺省为当天")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate verifyDate = date == null ? LocalDate.now() : date;
        return Result.ok(couponAdminService.listRecordOperators(
                LoginUserHolder.getRequired().getStoreId(), verifyDate));
    }
}
