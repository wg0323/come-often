package com.comeoften.module.stats.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 内部聚合行：区间内有支付单的会员数，以及其中复购会员数。
 */
@Data
public class StatsMemberAggRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long paidMemberCount;

    private Long repurchaseMemberCount;
}
