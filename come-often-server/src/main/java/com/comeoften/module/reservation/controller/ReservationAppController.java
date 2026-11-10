package com.comeoften.module.reservation.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.reservation.dto.ReservationCreateDTO;
import com.comeoften.module.reservation.dto.ReservationRescheduleDTO;
import com.comeoften.module.reservation.service.ReservationService;
import com.comeoften.module.reservation.vo.ReservationCodeVO;
import com.comeoften.module.reservation.vo.ReservationMetaVO;
import com.comeoften.module.reservation.vo.ReservationRoomVO;
import com.comeoften.module.reservation.vo.ReservationSlotVO;
import com.comeoften.module.reservation.vo.ReservationVO;
import com.comeoften.module.store.service.StoreService;
import com.comeoften.security.LoginUserHolder;
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
import java.util.List;

/**
 * 顾客端预约订座。会员 ID 取登录态，门店由服务端覆盖；容量与时段合法性全部由服务端校验。
 */
@Tag(name = "顾客端-预约订座", description = "门店时段查询、预约表单、我的预约、取消改期与签到码")
@RestController
@RequestMapping("/api/app/reservation")
@RequiredArgsConstructor
public class ReservationAppController {

    private final ReservationService reservationService;

    private final StoreService storeService;

    @Operation(summary = "预约页元数据", description = "可预约日期、时段、桌型与取消/超时规则")
    @ApiResponse(responseCode = "200", description = "返回元数据")
    @GetMapping("/meta")
    public Result<ReservationMetaVO> meta() {
        return Result.ok(reservationService.meta(storeService.defaultStoreId()));
    }

    @Operation(summary = "时段余量", description = "按日期查询各桌型容量、已占与剩余")
    @ApiResponse(responseCode = "200", description = "返回时段列表")
    @GetMapping("/slot")
    public Result<List<ReservationSlotVO>> slots(
            @Parameter(description = "预约日期，yyyy-MM-dd，缺省为当天")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(reservationService.slots(storeService.defaultStoreId(), date, false));
    }

    @Operation(summary = "可选包间", description = "按日期时段列出仍可锁定的包间；只有标了 room_type 的桌台会出现")
    @ApiResponse(responseCode = "200", description = "返回包间列表")
    @GetMapping("/rooms")
    public Result<List<ReservationRoomVO>> rooms(
            @Parameter(description = "预约日期，yyyy-MM-dd，缺省为当天")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "时段，HH:mm", required = true) @RequestParam String timeSlot,
            @Parameter(description = "就餐人数，用于按容量过滤") @RequestParam(required = false)
            Integer peopleCount) {
        return Result.ok(reservationService.rooms(storeService.defaultStoreId(), date, timeSlot, peopleCount));
    }

    @Operation(summary = "创建预约", description = "时段已满返回 30041，人数与桌型不匹配返回 30044；"
            + "传 tableId 时锁具体包间，包间已占用返回 30041")
    @ApiResponse(responseCode = "200", description = "返回预约单")
    @PostMapping
    public Result<ReservationVO> create(@Valid @RequestBody ReservationCreateDTO dto) {
        return Result.ok(reservationService.create(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), dto));
    }

    @Operation(summary = "我的预约", description = "tab 取 active / finished / cancelled，其余值返回全部")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @GetMapping("/page")
    public Result<PageResult<ReservationVO>> page(
            @Parameter(description = "Tab：active / finished / cancelled") @RequestParam(required = false) String tab,
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(reservationService.pageForMember(LoginUserHolder.getRequired().getUserId(),
                tab, current, size));
    }

    @Operation(summary = "预约详情", description = "含状态日志；非本人预约返回 30047")
    @ApiResponse(responseCode = "200", description = "返回预约详情")
    @GetMapping("/{reserveNo}")
    public Result<ReservationVO> detail(
            @Parameter(description = "预约单号", required = true) @PathVariable String reserveNo) {
        return Result.ok(reservationService.detailForMember(LoginUserHolder.getRequired().getUserId(),
                reserveNo));
    }

    @Operation(summary = "取消预约", description = "距预约不足 2 小时返回 30045")
    @ApiResponse(responseCode = "200", description = "返回取消后的预约单")
    @PostMapping("/{reserveNo}/cancel")
    public Result<ReservationVO> cancel(
            @Parameter(description = "预约单号", required = true) @PathVariable String reserveNo) {
        return Result.ok(reservationService.cancel(LoginUserHolder.getRequired().getUserId(), reserveNo));
    }

    @Operation(summary = "预约改期", description = "先占新时段再放旧时段，任一失败整体回滚")
    @ApiResponse(responseCode = "200", description = "返回改期后的预约单")
    @PostMapping("/{reserveNo}/reschedule")
    public Result<ReservationVO> reschedule(
            @Parameter(description = "预约单号", required = true) @PathVariable String reserveNo,
            @Valid @RequestBody ReservationRescheduleDTO dto) {
        return Result.ok(reservationService.reschedule(LoginUserHolder.getRequired().getUserId(),
                reserveNo, dto));
    }

    @Operation(summary = "签到码", description = "30 秒一变，带 R 前缀供商家端扫码分流")
    @ApiResponse(responseCode = "200", description = "返回签到码与本窗口剩余秒数")
    @GetMapping("/{reserveNo}/code")
    public Result<ReservationCodeVO> code(
            @Parameter(description = "预约单号", required = true) @PathVariable String reserveNo) {
        return Result.ok(reservationService.checkinCode(LoginUserHolder.getRequired().getUserId(), reserveNo));
    }
}
