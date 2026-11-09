package com.comeoften.mq.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 本地消息表后台视图。不含 payload 原文，避免敏感数据出现在接口与日志。
 */
@Data
@Schema(description = "本地消息记录")
public class MqMessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "消息业务 ID")
    private String messageId;

    @Schema(description = "目标交换机")
    private String exchangeName;

    @Schema(description = "路由键")
    private String routingKey;

    @Schema(description = "状态：PENDING/SENT/FAILED/DEAD")
    private String status;

    @Schema(description = "已重发次数")
    private Integer retryCount;

    @Schema(description = "下次补偿重发时间")
    private LocalDateTime nextRetryTime;

    @Schema(description = "最近一次失败原因")
    private String errorCause;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
