package com.comeoften.module.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.config.MerchantProperties;
import com.comeoften.module.merchant.service.MerchantNotifyService;
import com.comeoften.module.merchant.vo.MerchantNotifyVO;
import com.comeoften.module.order.constant.OrderStatus;
import com.comeoften.module.order.entity.TradeOrder;
import com.comeoften.module.order.mapper.TradeOrderMapper;
import com.comeoften.mq.service.NotifyRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * 商家端新订单提醒实现。
 *
 * <p>真实订阅消息需要商家端自己的模板 ID 与员工一次性订阅授权，M9 只做到「埋点 + 降级落库」：
 * 无论模板是否配置，实际通道都记 LOG、{@code sent} 恒为 false，只有 {@code templateConfigured}
 * 反映配置现状；真实发送通道留 M11，不接第三方推送、不做长连接。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantNotifyServiceImpl implements MerchantNotifyService {

    private static final String CHANNEL_LOG = "LOG";

    /** 只有「店里还没处理完」的订单才需要提醒，已取消 / 退款中 / 已完成都不提醒。 */
    private static final Set<OrderStatus> NOTIFIABLE = Collections.unmodifiableSet(
            EnumSet.of(OrderStatus.PAID, OrderStatus.ACCEPTED, OrderStatus.READY));

    private final TradeOrderMapper tradeOrderMapper;

    private final NotifyRecordService notifyRecordService;

    private final MerchantProperties merchantProperties;

    @Override
    public MerchantNotifyVO remindNewOrder(Long storeId, String orderNo, Long operatorUserId) {
        TradeOrder order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getOrderNo, orderNo));
        if (order == null || !storeId.equals(order.getStoreId())) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        OrderStatus status = OrderStatus.of(order.getStatus());
        if (status == null || !NOTIFIABLE.contains(status)) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "该订单当前状态不需要提醒");
        }
        String content = buildContent(order);
        boolean templateConfigured = StringUtils.hasText(merchantProperties.getSubscribeTemplateId());
        notifyRecordService.saveMerchantNewOrderNotice(storeId, orderNo, content);
        MerchantNotifyVO vo = new MerchantNotifyVO();
        vo.setSent(Boolean.FALSE);
        vo.setChannel(CHANNEL_LOG);
        vo.setTemplateConfigured(templateConfigured);
        vo.setOrderNo(orderNo);
        vo.setContent(content);
        log.info("商家端新订单提醒处理完成: storeId={}, orderNo={}, operatorUserId={}, templateConfigured={}",
                storeId, orderNo, operatorUserId, templateConfigured);
        return vo;
    }

    private String buildContent(TradeOrder order) {
        BigDecimal payAmount = order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount();
        return String.format(Locale.ROOT, "新订单 %s（取餐码 %s）待处理，实付 %s 元",
                order.getOrderNo(), order.getPickupCode() == null ? "-" : order.getPickupCode(),
                payAmount.stripTrailingZeros().toPlainString());
    }
}
