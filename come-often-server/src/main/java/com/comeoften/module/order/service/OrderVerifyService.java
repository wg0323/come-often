package com.comeoften.module.order.service;

import com.comeoften.common.PageResult;
import com.comeoften.module.order.constant.OperatorType;
import com.comeoften.module.order.entity.TradeOrder;
import com.comeoften.module.order.vo.OrderVerifyRecordVO;

/**
 * 订单核销审计。在订单状态推进到 COMPLETED 的同事务内落一条，重复推进由唯一键兜底。
 */
public interface OrderVerifyService {

    /**
     * 记录一次核销。幂等键为 {orderNo}#PICKUP，套餐单用 {orderNo}#BUNDLE。
     */
    void record(TradeOrder order, OperatorType operatorType, Long operatorId);

    PageResult<OrderVerifyRecordVO> pageForMember(Long memberId, Long storeId, Long current, Long size);

    OrderVerifyRecordVO detailForMember(Long memberId, Long storeId, Long recordId);
}
