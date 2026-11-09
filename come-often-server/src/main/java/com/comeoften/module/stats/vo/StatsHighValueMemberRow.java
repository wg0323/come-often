package com.comeoften.module.stats.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 内部聚合行：高价值用户排行的原始数据，phone 为明文，出接口前必须脱敏。
 */
@Data
public class StatsHighValueMemberRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long memberId;

    private String nickname;

    private String phone;

    private String levelName;

    private Long orderCount;

    private BigDecimal paidAmount;

    private BigDecimal refundAmount;

    private LocalDateTime lastPaidTime;
}
