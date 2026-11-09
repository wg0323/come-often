package com.comeoften.integration.notify.impl;

import com.comeoften.integration.notify.NotifyCommand;
import com.comeoften.integration.notify.NotifySendResult;
import com.comeoften.integration.notify.NotifySender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 微信订阅消息通道骨架（计划 3.10）。**当前不会真的下发**：
 * 项目没有订阅消息模板 ID、没有用户 openid → 模板参数的映射规则，也无法替用户完成
 * 一次性订阅授权（必须在小程序点击动作里调 {@code wx.requestSubscribeMessage}）。
 *
 * <p>因此这里只做两件事：记一条 warn 说明「模板未配置」，然后把通知交给 LOG 通道落库，
 * 保证 message_record 不丢。接真实发送时替换 {@link #send} 的实现并补齐模板 ID 与 openid 查询，
 * 调用方（NotifyRecordService 与各消费者）不用改。</p>
 */
@Slf4j
@RequiredArgsConstructor
public class WxSubscribeSender implements NotifySender {

    private final LogNotifySender logFallback;

    private final boolean templateConfigured;

    @Override
    public NotifySendResult send(NotifyCommand command) {
        log.warn("订阅消息通道未接入真实下发，降级为落库: type={}, templateConfigured={}, memberId={}, storeId={}",
                command.getMessageType(), templateConfigured, command.getMemberId(), command.getStoreId());
        return logFallback.send(command);
    }
}
