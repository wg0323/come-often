package com.comeoften.module.merchant.controller;

import com.comeoften.common.Result;
import com.comeoften.module.merchant.dto.TableStatusDTO;
import com.comeoften.module.reservation.service.ReservationService;
import com.comeoften.module.reservation.vo.ReservationTableBoardVO;
import com.comeoften.module.store.service.StoreService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;

/**
 * 商家端桌台看板。看板数据复用 M7 的 tableBoard（桌台三态 + 当日预约明细），
 * M9 只补一个改状态的窄接口：桌号、桌型与容量仍只在 PC 后台维护。
 */
@Tag(name = "商家端-桌台看板", description = "桌台三态宫格、包间人数与预约人、点格改状态")
@RestController
@RequestMapping("/api/merchant/table")
@RequiredArgsConstructor
public class MerchantTableController {

    private final ReservationService reservationService;

    private final StoreService storeService;

    @Operation(summary = "桌台看板", description = "当日桌台三态与未取消的预约明细，包间人数与预约人从预约明细取")
    @ApiResponse(responseCode = "200", description = "返回桌台看板")
    @RequirePerm("table:manage")
    @GetMapping("/board")
    public Result<ReservationTableBoardVO> board(
            @Parameter(description = "日期 yyyy-MM-dd，缺省为当天") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(reservationService.tableBoard(LoginUserHolder.getRequired().getStoreId(), date));
    }

    @Operation(summary = "改桌台状态", description = "0-空闲，1-占用，2-已预约；只改状态，不改桌号与容量")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @RequirePerm("table:manage")
    @PutMapping("/{tableId}/status")
    public Result<Void> updateStatus(
            @Parameter(description = "桌台 ID", required = true) @PathVariable Long tableId,
            @Valid @RequestBody TableStatusDTO dto) {
        storeService.updateTableStatus(LoginUserHolder.getRequired().getStoreId(), tableId, dto.getStatus());
        return Result.ok();
    }
}
