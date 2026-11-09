package com.comeoften.module.payment.converter;

import com.comeoften.module.order.entity.TradeOrder;
import com.comeoften.module.payment.entity.RefundRecord;
import com.comeoften.module.payment.vo.RefundVO;

/**
 * 退款单转换。状态码翻译与关联订单号属于非等价映射，集中在这里，分页映射也复用。
 */
public final class RefundConverter {

    private RefundConverter() {
    }

    public static RefundVO toVO(RefundRecord record, TradeOrder order) {
        RefundVO vo = new RefundVO();
        vo.setId(record.getId());
        vo.setRefundNo(record.getRefundNo());
        vo.setOrderNo(order == null ? "" : order.getOrderNo());
        vo.setOrderStatus(order == null ? "" : order.getStatus());
        vo.setRefundAmount(record.getRefundAmount());
        vo.setReason(record.getReason());
        vo.setStatus(record.getStatus());
        vo.setStatusText(statusText(record.getStatus()));
        vo.setAuditUserId(record.getAuditUserId());
        vo.setRefundTime(record.getRefundTime());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    public static String statusText(Integer status) {
        if (Integer.valueOf(1).equals(status)) {
            return "已退款";
        }
        if (Integer.valueOf(2).equals(status)) {
            return "已拒绝";
        }
        return "待审核";
    }
}
