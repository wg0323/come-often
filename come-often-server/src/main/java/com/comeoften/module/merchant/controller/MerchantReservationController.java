package com.comeoften.module.merchant.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.reservation.dto.ReservationRejectDTO;
import com.comeoften.module.reservation.service.ReservationService;
import com.comeoften.module.reservation.vo.ReservationVO;
import com.comeoften.security.LoginUserHolder;
import com.comeoften.security.RequirePerm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;

/**
 * 商家端预约管理。审核、拒绝与签到全部复用 M7 的 ReservationService，
 * 签到码走统一扫码入口（scan/resolve），这里只提供按单号签到。
 */
@Tag(name = "商家端-预约管理", description = "四 Tab 预约列表、确认 / 拒绝与到店签到")
@RestController
@RequestMapping("/api/merchant/reservation")
@RequiredArgsConstructor
public class MerchantReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "预约分页", description = "按日期与状态过滤：PENDING / CONFIRMED / CHECKED_IN / NO_SHOW")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @RequirePerm("reservation:audit")
    @GetMapping("/page")
    public Result<PageResult<ReservationVO>> page(
            @Parameter(description = "预约日期 yyyy-MM-dd，可空") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "状态过滤") @RequestParam(required = false) String status,
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数，上限 50") @RequestParam(defaultValue = "20") Long size) {
        return Result.ok(reservationService.adminPage(LoginUserHolder.getRequired().getStoreId(),
                date, status, null, current, size));
    }

    @Operation(summary = "确认预约", description = "PENDING→CONFIRMED；其他状态返回 30046")
    @ApiResponse(responseCode = "200", description = "返回确认后的预约单")
    @RequirePerm("reservation:audit")
    @PostMapping("/{reserveNo}/confirm")
    public Result<ReservationVO> confirm(
            @Parameter(description = "预约单号", required = true) @PathVariable String reserveNo) {
        return Result.ok(reservationService.confirm(LoginUserHolder.getRequired().getStoreId(),
                LoginUserHolder.getRequired().getUserId(), reserveNo));
    }

    @Operation(summary = "拒绝预约", description = "置为 CANCELLED 并释放时段，原因写入状态日志")
    @ApiResponse(responseCode = "200", description = "返回拒绝后的预约单")
    @RequirePerm("reservation:audit")
    @PostMapping("/{reserveNo}/reject")
    public Result<ReservationVO> reject(
            @Parameter(description = "预约单号", required = true) @PathVariable String reserveNo,
            @Valid @RequestBody ReservationRejectDTO dto) {
        return Result.ok(reservationService.reject(LoginUserHolder.getRequired().getStoreId(),
                LoginUserHolder.getRequired().getUserId(), reserveNo, dto.getRemark()));
    }

    @Operation(summary = "到店签到", description = "按预约单号签到，写 checkin_user_id 与 checkin_time；可同时绑定桌台")
    @ApiResponse(responseCode = "200", description = "返回签到后的预约单")
    @RequirePerm("reservation:audit")
    @PostMapping("/{reserveNo}/checkin")
    public Result<ReservationVO> checkin(
            @Parameter(description = "预约单号", required = true) @PathVariable String reserveNo,
            @Parameter(description = "绑定的桌台 ID，可空") @RequestParam(required = false) Long tableId) {
        return Result.ok(reservationService.checkin(LoginUserHolder.getRequired().getStoreId(),
                LoginUserHolder.getRequired().getUserId(), reserveNo, tableId));
    }
}
