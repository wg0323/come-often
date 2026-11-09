package com.comeoften.mq.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知 / 告警发送记录。M3 的 LogNotifySender 与死信告警统一落这张表。
 */
@Data
@TableName("message_record")
public class MessageRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long memberId;
    private Long storeId;
    private String messageType;
    private String title;
    private String content;
    private String channel;
    private Integer sendStatus;
    private LocalDateTime sendTime;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
