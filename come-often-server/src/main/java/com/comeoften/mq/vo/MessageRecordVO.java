package com.comeoften.mq.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知 / 告警记录视图。死信告警与投递失败告警都落 message_record(MQ_ALARM)，
 * 后台按类型分页查这张表即可定位「哪条消息、哪个队列、什么时候」。
 */
@Data
@Schema(description = "通知与告警记录")
public class MessageRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "记录 ID")
    private Long id;

    @Schema(description = "通知类型：ORDER_STATUS / PAY_SUCCEEDED / COUPON_EXPIRE / RESERVATION_REMIND"
            + " / MERCHANT_NEW_ORDER / MQ_ALARM")
    private String messageType;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容摘要，超长截断到 480 字")
    private String content;

    @Schema(description = "通道：当前恒为 LOG")
    private String channel;

    @Schema(description = "发送状态：1-已落库/已发送")
    private Integer sendStatus;

    @Schema(description = "发送时间")
    private LocalDateTime sendTime;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "会员 ID")
    private Long memberId;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "门店 ID")
    private Long storeId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
