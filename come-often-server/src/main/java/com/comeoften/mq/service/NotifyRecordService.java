package com.comeoften.mq.service;

import com.comeoften.common.PageResult;
import com.comeoften.mq.message.CouponExpireMessage;
import com.comeoften.mq.message.OrderEventMessage;
import com.comeoften.mq.message.ReservationMessage;
import com.comeoften.mq.vo.MessageRecordVO;

/**
 * 通知与告警落库。默认 LogNotifySender 语义：写 message_record 并打日志，不阻塞下单主流程。
 */
public interface NotifyRecordService {

    void saveOrderStatusNotice(OrderEventMessage message);

    /**
     * M4 支付成功事件落库。M5/M6 后续在此消费者上接发券与积分，先保留可查的事件痕迹。
     */
    void savePaySucceededEvent(OrderEventMessage message);

    /**
     * M5 券即将过期提醒落库。真实订阅消息同样需要模板 ID，这里沿用「落库 + 日志」形态。
     */
    void saveCouponExpireNotice(CouponExpireMessage message);

    /**
     * M7 预约到店提醒落库。与券过期提醒同形态：模板 ID 未接入前只落 message_record 并打日志。
     */
    void saveReservationRemindNotice(ReservationMessage message);

    /**
     * M9 商家端新订单提醒落库。商家端与顾客端分属两套订阅消息模板，
     * 模板未接入时沿用「落库 + 日志」形态，前台轮询仍可正常提醒。
     */
    void saveMerchantNewOrderNotice(Long storeId, String orderNo, String content);

    void saveAlarm(String title, String content);

    /**
     * 分页查 {@code MQ_ALARM} 告警记录（死信与投递失败都落这个类型）。
     *
     * <p>刻意不开放「按任意 messageType 查」：业务通知里带订单号、会员 ID、门店 ID，
     * 而 {@code order:manage} 是门店级权限，开放类型过滤等于让 A 店店长读到 B 店的业务数据。
     * 告警记录本身不带 memberId / storeId，只描述消息元信息，可以放心给运维看。</p>
     */
    PageResult<MessageRecordVO> pageAlarm(Long current, Long size);
}
