package com.comeoften.module.balance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员储值账户，一个会员一行。余额与流水严格对账：{@code balance = Σ balance_record.change_amount}。
 */
@Data
@TableName("member_balance")
public class MemberBalance implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long memberId;

    /** 可用储值余额，单位元。 */
    private BigDecimal balance;

    /** 累计充值本金，充值挂预收，不计入营收。 */
    private BigDecimal totalRecharge;

    /** 累计赠送金额。 */
    private BigDecimal totalGift;

    /** 累计余额消费。 */
    private BigDecimal totalConsume;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
