package com.comeoften.module.queue.controller;

import com.comeoften.common.Result;
import com.comeoften.module.queue.dto.QueueTicketCreateDTO;
import com.comeoften.module.queue.service.QueueTicketService;
import com.comeoften.module.queue.vo.QueueTicketVO;
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

/**
 * 顾客端排队叫号：取号、查当前号与取消。门店由服务端按默认门店覆盖。
 */
@Tag(name = "顾客端-排队叫号", description = "现场取号、我的号与取消")
@RestController
@RequestMapping("/api/app/queue")
@RequiredArgsConstructor
public class QueueAppController {

    private final QueueTicketService queueTicketService;

    private final StoreService storeService;

    @Operation(summary = "取号", description = "同一会员当天已有等待中或已叫号的号时返回 30059")
    @ApiResponse(responseCode = "200", description = "返回取到的排队号")
    @PostMapping("/ticket")
    public Result<QueueTicketVO> take(@Valid @RequestBody QueueTicketCreateDTO dto) {
        return Result.ok(queueTicketService.take(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), dto.getPartySize()));
    }

    @Operation(summary = "我的排队号", description = "当天最新的有效号；没有取号时 data 为 null")
    @ApiResponse(responseCode = "200", description = "返回当前排队号")
    @GetMapping("/current")
    public Result<QueueTicketVO> current() {
        return Result.ok(queueTicketService.current(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId()));
    }

    @Operation(summary = "取消排队号", description = "只能取消自己的、仍处于等待中或已叫号的号")
    @ApiResponse(responseCode = "200", description = "返回取消后的排队号")
    @PostMapping("/ticket/{ticketNo}/cancel")
    public Result<QueueTicketVO> cancel(
            @Parameter(description = "排队号", required = true) @PathVariable String ticketNo) {
        return Result.ok(queueTicketService.cancel(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), ticketNo));
    }
}
