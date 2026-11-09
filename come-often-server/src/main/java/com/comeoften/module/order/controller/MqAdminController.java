package com.comeoften.module.order.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.mq.MessageProducer;
import com.comeoften.mq.service.MqMessageService;
import com.comeoften.mq.service.NotifyRecordService;
import com.comeoften.mq.vo.MessageRecordVO;
import com.comeoften.mq.vo.MqMessageVO;
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
 * 本地消息表与死信告警面板。需要 order:manage 权限点。
 */
@Tag(name = "PC 后台-消息可靠性", description = "本地消息表、失败与死信告警、人工重放")
@RestController
@RequestMapping("/api/admin/mq/message")
@RequiredArgsConstructor
public class MqAdminController {

    private final MqMessageService mqMessageService;

    private final NotifyRecordService notifyRecordService;

    private final MessageProducer messageProducer;

    @Operation(summary = "本地消息分页列表", description = "可按 PENDING/SENT/FAILED/DEAD 过滤，不含消息体原文")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @RequirePerm("order:manage")
    @GetMapping
    public Result<PageResult<MqMessageVO>> page(
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "状态过滤") @RequestParam(required = false) String status) {
        return Result.ok(mqMessageService.pageForAdmin(current, size, status));
    }

    @Operation(summary = "死信与投递失败告警分页",
            description = "只读 message_record 的 MQ_ALARM 类型：死信与重发耗尽的告警都落这里，"
                    + "记录不含会员与门店业务字段")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @RequirePerm("order:manage")
    @GetMapping("/alarm")
    public Result<PageResult<MessageRecordVO>> alarmPage(
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(notifyRecordService.pageAlarm(current, size));
    }

    @Operation(summary = "人工重放本地消息", description = "按 messageId 从本地消息表还原并重新投递")
    @ApiResponse(responseCode = "200", description = "重放请求已提交")
    @RequirePerm("order:manage")
    @PostMapping("/{messageId}/republish")
    public Result<Void> republish(
            @Parameter(description = "消息业务 ID", required = true) @PathVariable String messageId) {
        messageProducer.republish(messageId);
        return Result.ok();
    }
}
