package com.comeoften.mq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comeoften.common.PageResult;
import com.comeoften.integration.notify.NotifyCommand;
import com.comeoften.integration.notify.NotifySender;
import com.comeoften.module.order.constant.OrderStatus;
import com.comeoften.mq.entity.MessageRecord;
import com.comeoften.mq.mapper.MessageRecordMapper;
import com.comeoften.mq.message.CouponExpireMessage;
import com.comeoften.mq.message.OrderEventMessage;
import com.comeoften.mq.message.ReservationMessage;
import com.comeoften.mq.service.NotifyRecordService;
import com.comeoften.mq.vo.MessageRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 通知与告警的业务侧收口：只负责「通知什么」，不再关心「怎么发」。
 * 发送通道由 {@link NotifySender} 决定（默认 LOG 落库，订阅消息为骨架），
 * 换真实通道时本类不改。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyRecordServiceImpl implements NotifyRecordService {

    private static final String TYPE_ORDER_STATUS = "ORDER_STATUS";

    private static final String TYPE_PAY_SUCCEEDED = "PAY_SUCCEEDED";

    private static final String TYPE_MQ_ALARM = "MQ_ALARM";

    private static final String TYPE_COUPON_EXPIRE = "COUPON_EXPIRE";

    private static final String TYPE_RESERVATION_REMIND = "RESERVATION_REMIND";

    private static final String TYPE_MERCHANT_NEW_ORDER = "MERCHANT_NEW_ORDER";

    private static final String TYPE_QUEUE_CALL = "QUEUE_CALL";

    private static final int MAX_CONTENT_LENGTH = 480;

    private final NotifySender notifySender;

    private final MessageRecordMapper messageRecordMapper;

    @Override
    public void saveOrderStatusNotice(OrderEventMessage message) {
        OrderStatus status = OrderStatus.of(message.getToStatus());
        String statusText = status == null ? message.getToStatus() : status.getDescription();
        notifySender.send(build(message.getMessageId(), TYPE_ORDER_STATUS, "订单状态更新",
                "订单 " + message.getOrderNo() + " 已" + statusText,
                message.getMemberId(), message.getStoreId()));
    }

    @Override
    public void savePaySucceededEvent(OrderEventMessage message) {
        notifySender.send(build(message.getMessageId(), TYPE_PAY_SUCCEEDED, "支付成功事件",
                "订单 " + message.getOrderNo() + " 支付成功，供后续发券与积分消费",
                message.getMemberId(), message.getStoreId()));
    }

    @Override
    public void saveCouponExpireNotice(CouponExpireMessage message) {
        notifySender.send(build(message.getMessageId(), TYPE_COUPON_EXPIRE, "优惠券即将过期",
                "您有一张「" + message.getTemplateName() + "」即将过期，请尽快使用",
                message.getMemberId(), message.getStoreId()));
    }

    @Override
    public void saveReservationRemindNotice(ReservationMessage message) {
        notifySender.send(build(message.getMessageId(), TYPE_RESERVATION_REMIND, "预约即将开始",
                "您预约的 " + message.getReserveDate() + " " + message.getTimeSlot()
                        + " 座位即将开始，请按时到店并出示签到码",
                message.getMemberId(), message.getStoreId()));
    }

    @Override
    public void saveMerchantNewOrderNotice(Long storeId, String orderNo, String content) {
        notifySender.send(build(null, TYPE_MERCHANT_NEW_ORDER, "新订单提醒", trim(content), null, storeId));
    }

    @Override
    public void saveQueueCalledNotice(Long memberId, Long storeId, String ticketNo, Integer partySize) {
        String size = partySize == null ? "" : "（" + partySize + " 位）";
        notifySender.send(build(null, TYPE_QUEUE_CALL, "排队叫号提醒",
                "您的排队号 " + ticketNo + " 已叫号" + size + "，请尽快到前台", memberId, storeId));
    }

    @Override
    public void saveAlarm(String title, String content) {
        notifySender.send(build(null, TYPE_MQ_ALARM, title, trim(content), null, null));
    }

    @Override
    public PageResult<MessageRecordVO> pageAlarm(Long current, Long size) {
        // 只查 MQ_ALARM：业务通知带订单号与会员/门店 ID，按类型开放会越权读到别店数据
        IPage<MessageRecord> result = messageRecordMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<MessageRecord>()
                        .eq(MessageRecord::getMessageType, TYPE_MQ_ALARM)
                        .orderByDesc(MessageRecord::getCreateTime));
        List<MessageRecordVO> records = new ArrayList<>(result.getRecords().size());
        for (MessageRecord entity : result.getRecords()) {
            MessageRecordVO vo = new MessageRecordVO();
            BeanUtils.copyProperties(entity, vo);
            records.add(vo);
        }
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    private static NotifyCommand build(String messageId, String messageType, String title, String content,
                                       Long memberId, Long storeId) {
        NotifyCommand command = new NotifyCommand();
        command.setMessageId(messageId);
        command.setMessageType(messageType);
        command.setTitle(title);
        command.setContent(trim(content));
        command.setMemberId(memberId);
        command.setStoreId(storeId);
        return command;
    }

    private static String trim(String content) {
        String value = content == null ? "" : content;
        return value.length() > MAX_CONTENT_LENGTH ? value.substring(0, MAX_CONTENT_LENGTH) : value;
    }
}
