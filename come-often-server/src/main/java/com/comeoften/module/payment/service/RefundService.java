package com.comeoften.module.payment.service;

import com.comeoften.common.PageResult;
import com.comeoften.module.payment.dto.RefundApplyDTO;
import com.comeoften.module.payment.dto.RefundAuditDTO;
import com.comeoften.module.payment.vo.RefundVO;

/**
 * 退款编排。制作前（PAID）可直接退，制作中 / 待取餐（ACCEPTED / READY）进 REFUNDING 等后台审核。
 * 订单状态迁移统一委托 OrderService.transition，不绕过 OrderStatusTransition 与带旧状态条件的 UPDATE。
 */
public interface RefundService {

    RefundVO applyByMember(Long memberId, Long storeId, String orderNo, RefundApplyDTO dto);

    RefundVO applyByAdmin(Long storeId, String orderNo, RefundApplyDTO dto, Long operatorId);

    RefundVO audit(Long storeId, String refundNo, RefundAuditDTO dto, Long operatorId);

    RefundVO detailForMember(Long memberId, Long storeId, String orderNo);

    PageResult<RefundVO> pageForAdmin(Long storeId, Long current, Long size, Integer status);
}
