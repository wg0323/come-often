package com.comeoften.mq.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 本地消息表。业务事务内写 PENDING，事务提交后投递，confirm 成功置 SENT。
 */
@Data
@TableName("mq_message")
public class MqMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String messageId;
    private String exchangeName;
    private String routingKey;
    private String payload;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private String errorCause;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
