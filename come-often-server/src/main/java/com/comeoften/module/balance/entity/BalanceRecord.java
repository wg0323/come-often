package com.comeoften.module.balance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 储值余额流水。{@code uk_biz_idempotent(member_id, biz_type, biz_id)} 保证同一业务只入账一次，
 * biz_id 沿用 M6 的 {@code {业务号}#用途} 后缀口径。
 */
@Data
@TableName("balance_record")
public class BalanceRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long memberId;

    /** 方向：RECHARGE / GIFT / CONSUME / REFUND / ROLLBACK。 */
    private String changeType;

    /** 变动金额，正数入账负数出账，单位元。 */
    private BigDecimal changeAmount;

    /** 变动后可用余额，单位元。 */
    private BigDecimal balanceAfter;

    /** 业务类型：RECHARGE / ORDER / REFUND。 */
    private String bizType;

    /** 业务幂等键，如 {orderNo}#DEDUCT / {orderNo}#ROLLBACK。 */
    private String bizId;

    private String remark;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
