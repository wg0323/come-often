package com.comeoften.module.balance.service;

import com.comeoften.common.PageResult;
import com.comeoften.module.balance.vo.BalanceMemberVO;
import com.comeoften.module.balance.vo.BalanceRecordVO;

/**
 * PC 后台储值查询。只读：余额变动全部由顾客充值与订单链路产生，后台不提供手工加减余额。
 */
public interface BalanceAdminService {

    /**
     * 储值会员分页，keyword 命中昵称或手机号。
     */
    PageResult<BalanceMemberVO> memberPage(Long current, Long size, String keyword);

    /**
     * 储值流水分页，memberId 为空查全部。
     */
    PageResult<BalanceRecordVO> recordPage(Long memberId, Long current, Long size);
}
