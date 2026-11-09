package com.comeoften.module.reservation.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.reservation.dto.ReservationCheckinDTO;
import com.comeoften.module.reservation.dto.ReservationRejectDTO;
import com.comeoften.module.reservation.service.ReservationService;
import com.comeoften.module.reservation.service.ReservationSlotService;
import com.comeoften.module.reservation.vo.ReservationCalendarDayVO;
import com.comeoften.module.reservation.vo.ReservationSlotVO;
import com.comeoften.module.reservation.vo.ReservationTableBoardVO;
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
import java.util.List;

/**
 * PC 后台预约管理。权限点复用 M1 菜单 103 的 reservation:manage，
 * 商家端 M9 的 reservation:audit / table:manage 不在此实现。
 */
@Tag(name = "PC 后台-预约管理", description = "日历视图、审核、签到、桌台看板与时段生成")
@RestController
@RequestMapping("/api/admin/reservation")
@RequiredArgsConstructor
public class ReservationAdminController {

    private final ReservationService reservationService;

    private final ReservationSlotService reservationSlotService;

    @Operation(summary = "预约分页", description = "按日期、状态与关键词过滤，关键词按前缀匹配")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @RequirePerm("reservation:manage")
    @GetMapping("/page")
    public Result<PageResult<ReservationVO>> page(
            @Parameter(description = "预约日期，yyyy-MM-dd") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "状态：PENDING/CONFIRMED/CHECKED_IN/NO_SHOW/CANCELLED")
            @RequestParam(required = false) String status,
            @Parameter(description = "预约单号 / 联系人 / 手机号前缀") @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(reservationService.adminPage(LoginUserHolder.getRequired().getStoreId(),
                date, status, keyword, current, size));
    }

    @Operation(summary = "预约详情", description = "含会员信息与状态日志")
    @ApiResponse(responseCode = "200", description = "返回预约详情")
    @RequirePerm("reservation:manage")
    @GetMapping("/{reserveNo}")
    public Result<ReservationVO> detail(
            @Parameter(description = "预约单号", required = true) @PathVariable String reserveNo) {
        return Result.ok(reservationService.adminDetail(LoginUserHolder.getRequired().getStoreId(), reserveNo));
    }

    @Operation(summary = "确认预约", description = "PENDING→CONFIRMED；其他状态返回 30046")
    @ApiResponse(responseCode = "200", description = "返回确认后的预约单")
    @RequirePerm("reservation:manage")
    @PostMapping("/{reserveNo}/confirm")
    public Result<ReservationVO> confirm(
            @Parameter(description = "预约单号", required = true) @PathVariable String reserveNo) {
        return Result.ok(reservationService.confirm(LoginUserHolder.getRequired().getStoreId(),
                LoginUserHolder.getRequired().getUserId(), reserveNo));
    }

    @Operation(summary = "拒绝预约", description = "置为 CANCELLED 并释放时段，原因写入状态日志")
    @ApiResponse(responseCode = "200", description = "返回拒绝后的预约单")
    @RequirePerm("reservation:manage")
    @PostMapping("/{reserveNo}/reject")
    public Result<ReservationVO> reject(
            @Parameter(description = "预约单号", required = true) @PathVariable String reserveNo,
            @Valid @RequestBody ReservationRejectDTO dto) {
        return Result.ok(reservationService.reject(LoginUserHolder.getRequired().getStoreId(),
                LoginUserHolder.getRequired().getUserId(), reserveNo, dto.getRemark()));
    }

    @Operation(summary = "后台签到", description = "按预约单号签到，可同时绑定具体桌台")
    @ApiResponse(responseCode = "200", description = "返回签到后的预约单")
    @RequirePerm("reservation:manage")
    @PostMapping("/{reserveNo}/checkin")
    public Result<ReservationVO> checkin(
            @Parameter(description = "预约单号", required = true) @PathVariable String reserveNo,
            @Parameter(description = "绑定的桌台 ID，可空") @RequestParam(required = false) Long tableId) {
        return Result.ok(reservationService.checkin(LoginUserHolder.getRequired().getStoreId(),
                LoginUserHolder.getRequired().getUserId(), reserveNo, tableId));
    }

    @Operation(summary = "扫码签到", description = "校验 R 前缀、签名与 30 秒时间窗；失败返回 30048")
    @ApiResponse(responseCode = "200", description = "返回签到后的预约单")
    @RequirePerm("reservation:manage")
    @PostMapping("/checkin-by-code")
    public Result<ReservationVO> checkinByCode(@Valid @RequestBody ReservationCheckinDTO dto) {
        return Result.ok(reservationService.checkinByCode(LoginUserHolder.getRequired().getStoreId(),
                LoginUserHolder.getRequired().getUserId(), dto));
    }

    @Operation(summary = "日历视图", description = "按日期区间统计各状态数量与容量概况，跨度不超过 31 天")
    @ApiResponse(responseCode = "200", description = "返回日历数据")
    @RequirePerm("reservation:manage")
    @GetMapping("/calendar")
    public Result<List<ReservationCalendarDayVO>> calendar(
            @Parameter(description = "开始日期，yyyy-MM-dd") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期，yyyy-MM-dd") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(reservationService.calendar(LoginUserHolder.getRequired().getStoreId(),
                startDate, endDate));
    }

    @Operation(summary = "桌台看板", description = "桌台三态、当日时段容量与当日预约明细")
    @ApiResponse(responseCode = "200", description = "返回桌台看板")
    @RequirePerm("reservation:manage")
    @GetMapping("/table-board")
    public Result<ReservationTableBoardVO> tableBoard(
            @Parameter(description = "日期，yyyy-MM-dd，缺省为当天") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(reservationService.tableBoard(LoginUserHolder.getRequired().getStoreId(), date));
    }

    @Operation(summary = "生成时段库存", description = "按未来 N 天 × 时段 × 桌型幂等生成，capacity 由 store_table 聚合")
    @ApiResponse(responseCode = "200", description = "返回本次处理的组合数")
    @RequirePerm("reservation:manage")
    @PostMapping("/slot/generate")
    public Result<Integer> generate(
            @Parameter(description = "生成天数，缺省取配置的 days-ahead") @RequestParam(required = false) Integer days) {
        return Result.ok(reservationSlotService.generate(LoginUserHolder.getRequired().getStoreId(), days));
    }

    @Operation(summary = "重建容量键", description = "按 DB booked 重建 Redis，供 Redis 重启或验收前对齐")
    @ApiResponse(responseCode = "200", description = "返回重建的 key 数")
    @RequirePerm("reservation:manage")
    @PostMapping("/slot/rebuild")
    public Result<Integer> rebuild() {
        return Result.ok(reservationSlotService.rebuildRedis(LoginUserHolder.getRequired().getStoreId()));
    }

    @Operation(summary = "时段余量（后台）", description = "按日期查询全部桌型的容量与占用")
    @ApiResponse(responseCode = "200", description = "返回时段列表")
    @RequirePerm("reservation:manage")
    @GetMapping("/slot")
    public Result<List<ReservationSlotVO>> slots(
            @Parameter(description = "日期，yyyy-MM-dd") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(reservationService.slots(LoginUserHolder.getRequired().getStoreId(), date, true));
    }

    @Operation(summary = "补发超时/提醒消息", description = "运维与验收用：kind 取 timeout / remind，走同一条消费链路")
    @ApiResponse(responseCode = "200", description = "补发成功")
    @RequirePerm("reservation:manage")
    @PostMapping("/{reserveNo}/probe")
    public Result<Void> probe(
            @Parameter(description = "预约单号", required = true) @PathVariable String reserveNo,
            @Parameter(description = "消息类型：timeout / remind") @RequestParam(defaultValue = "timeout") String kind,
            @Parameter(description = "延迟秒数，1~3600") @RequestParam(defaultValue = "3") Long delaySeconds) {
        reservationService.sendProbe(LoginUserHolder.getRequired().getStoreId(), reserveNo, kind, delaySeconds);
        return Result.ok();
    }
}
