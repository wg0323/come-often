package com.comeoften.module.balance.service;

import com.comeoften.common.PageResult;
import com.comeoften.module.balance.dto.BalanceRechargeDTO;
import com.comeoften.module.balance.vo.BalanceAccountVO;
import com.comeoften.module.balance.vo.BalanceRechargeVO;
import com.comeoften.module.balance.vo.BalanceRecordVO;

import java.math.BigDecimal;

/**
 * 会员储值。余额的唯一事实源是 {@code member_balance}，每次变动都在同一事务里写一条流水。
 */
public interface BalanceService {

    /**
     * 账户概览，含充值档位；无账户时返回零值账户。
     */
    BalanceAccountVO account(Long memberId);

    /**
     * 充值入账（当前为 Mock 渠道）：本金 + 档位赠送，两条流水。
     */
    BalanceRechargeVO recharge(Long memberId, BalanceRechargeDTO dto);

    /**
     * 顾客端储值流水。
     */
    PageResult<BalanceRecordVO> recordPage(Long memberId, Long current, Long size);

    /**
     * 可用余额，无账户返回 0。
     */
    BigDecimal availableOf(Long memberId);

    /**
     * 下单抵扣：带余额条件的实扣，余额不足抛 30064。
     */
    BigDecimal deductForOrder(Long memberId, String orderNo, BigDecimal amount);

    /**
     * 取消 / 超时退回抵扣，幂等。
     */
    void rollbackForOrder(Long memberId, String orderNo);

    /**
     * 整单退款把余额抵扣部分退回，幂等。
     */
    void refundForOrder(Long memberId, String orderNo, BigDecimal orderBalanceAmount);
}
