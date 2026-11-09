package com.comeoften.module.order.service;

import com.comeoften.common.PageResult;
import com.comeoften.module.order.constant.OperatorType;
import com.comeoften.module.order.dto.OrderCreateDTO;
import com.comeoften.module.order.dto.OrderPayDTO;
import com.comeoften.module.order.vo.OrderPreviewVO;
import com.comeoften.module.order.vo.OrderSummaryVO;
import com.comeoften.module.order.vo.OrderVO;

/**
 * 自提 / 到店订单。金额一律复用 M2 的 DishOrderService.check，页面不参与计算。
 */
public interface OrderService {

    OrderPreviewVO preview(Long memberId, Long storeId, OrderCreateDTO dto);

    OrderVO create(Long memberId, Long storeId, OrderCreateDTO dto);

    OrderVO detailForApp(Long memberId, Long storeId, String orderNo);

    OrderVO detailForAdmin(Long storeId, String orderNo);

    PageResult<OrderSummaryVO> pageForApp(Long memberId, Long storeId, Long current, Long size, String status);

    PageResult<OrderSummaryVO> pageForAdmin(Long storeId, Long current, Long size,
                                            String status, String keyword);

    /**
     * 发起支付：委托 PaymentService 走 PayGateway 预下单与回调入账，保持 CREATED→PAID 的返回契约。
     */
    OrderVO pay(Long memberId, Long storeId, String orderNo, OrderPayDTO dto);

    OrderVO cancel(Long memberId, Long storeId, String orderNo);

    /**
     * 后台 / 商家端状态推进。非法迁移由 OrderStatusTransition 抛 30002。
     */
    OrderVO transition(Long storeId, String orderNo, String toStatus,
                       String remark, OperatorType operatorType, Long operatorId);

    /**
     * 延迟消息到点触发的待支付超时取消。返回 false 表示订单不存在或已非 CREATED，属正常重复投递。
     */
    boolean cancelOnTimeout(String orderNo, String messageId);

    /**
     * 运维 / 验收补发一条短延迟的超时消息，用于验证延迟链路与库存回补。
     */
    void resendTimeoutMessage(Long storeId, String orderNo, long delaySeconds);
}
