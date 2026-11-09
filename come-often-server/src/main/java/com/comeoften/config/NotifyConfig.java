package com.comeoften.config;

import com.comeoften.integration.notify.NotifySender;
import com.comeoften.integration.notify.impl.LogNotifySender;
import com.comeoften.integration.notify.impl.WxSubscribeSender;
import com.comeoften.mq.mapper.MessageRecordMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 通知通道装配。默认 {@code comeoften.notify.channel=log}，只落库 + 打日志；
 * 置为 {@code wx-subscribe} 时换成订阅消息骨架——它同样会降级落库，区别只在日志里点明通道。
 *
 * <p>只装配一个 {@link NotifySender} Bean，避免将来加通道时出现多个候选需要 @Primary 打架。</p>
 */
@Configuration
public class NotifyConfig {

    private static final String CHANNEL_WX_SUBSCRIBE = "wx-subscribe";

    @Bean
    public NotifySender notifySender(MessageRecordMapper messageRecordMapper,
                                    @Value("${comeoften.notify.channel:log}") String channel,
                                    @Value("${comeoften.notify.wx-subscribe-template-id:}") String templateId) {
        LogNotifySender logSender = new LogNotifySender(messageRecordMapper);
        if (CHANNEL_WX_SUBSCRIBE.equalsIgnoreCase(channel)) {
            return new WxSubscribeSender(logSender, StringUtils.hasText(templateId));
        }
        return logSender;
    }
}
