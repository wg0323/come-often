package com.comeoften.module.queue.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 现场排队号。biz_date 是营业日，编号按门店 + 营业日从 Q001 递增。
 */
@Data
@TableName("queue_ticket")
public class QueueTicket implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long storeId;

    private Long memberId;

    private LocalDate bizDate;

    private String ticketNo;

    private Integer partySize;

    private String status;

    private LocalDateTime calledTime;

    private LocalDateTime seatedTime;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
