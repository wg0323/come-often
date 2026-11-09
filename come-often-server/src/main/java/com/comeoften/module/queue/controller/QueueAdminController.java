package com.comeoften.module.queue.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.queue.service.QueueTicketService;
import com.comeoften.module.queue.vo.QueueTicketVO;
import com.comeoften.module.store.service.StoreService;
import com.comeoften.security.RequirePerm;
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

/**
 * PC 后台排队看板。复用预约管理权限点，不新增菜单与权限数据。
 */
@Tag(name = "PC 后台-排队叫号", description = "当天排队号分页、叫号与入座")
@RestController
@RequestMapping("/api/admin/queue")
@RequiredArgsConstructor
public class QueueAdminController {

    private final QueueTicketService queueTicketService;

    private final StoreService storeService;

    @Operation(summary = "排队号分页", description = "只查当天；status 为空表示全部状态")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @RequirePerm("reservation:manage")
    @GetMapping("/page")
    public Result<PageResult<QueueTicketVO>> page(
            @Parameter(description = "状态：WAITING/CALLED/SEATED/CANCELLED/EXPIRED")
            @RequestParam(required = false) String status,
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(queueTicketService.page(storeService.defaultStoreId(), status, current, size));
    }

    @Operation(summary = "叫号", description = "WAITING → CALLED，重复点击返回 30058")
    @ApiResponse(responseCode = "200", description = "返回叫号后的排队号")
    @RequirePerm("reservation:manage")
    @PostMapping("/{ticketNo}/call")
    public Result<QueueTicketVO> call(
            @Parameter(description = "排队号", required = true) @PathVariable String ticketNo) {
        return Result.ok(queueTicketService.call(storeService.defaultStoreId(), ticketNo));
    }

    @Operation(summary = "入座", description = "CALLED → SEATED")
    @ApiResponse(responseCode = "200", description = "返回入座后的排队号")
    @RequirePerm("reservation:manage")
    @PostMapping("/{ticketNo}/seat")
    public Result<QueueTicketVO> seat(
            @Parameter(description = "排队号", required = true) @PathVariable String ticketNo) {
        return Result.ok(queueTicketService.seat(storeService.defaultStoreId(), ticketNo));
    }
}
