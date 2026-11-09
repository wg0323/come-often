package com.comeoften.mq;

import com.comeoften.mq.service.MqMessageService;
import com.comeoften.mq.service.NotifyRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 投递失败与死信的兜底出口。M3 起由「只打日志」替换为落库 + 告警：
 * 本地消息表标 FAILED / DEAD，并写 message_record(MQ_ALARM) 供后台面板查看。
 * 调用方（ConfirmCallback / ReturnsCallback / DeadLetterListener）签名保持不变。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqFailureHandler {

    private static final int MAX_CONTENT_LENGTH = 480;

    private final MqMessageService mqMessageService;

    private final NotifyRecordService notifyRecordService;

    public void saveSendFailure(String messageId, String exchange, String routingKey, String cause) {
        mqMessageService.markFailed(messageId, cause);
        log.error("消息最终投递失败待人工处理: messageId={}, exchange={}, routingKey={}, cause={}",
                messageId, exchange, routingKey, cause);
    }

    public void notifySendFailed(String messageId, String exchange, String routingKey) {
        notifyRecordService.saveAlarm("消息投递失败告警",
                trim("messageId=" + messageId + ", exchange=" + exchange + ", routingKey=" + routingKey));
        log.error("告警-消息投递失败: messageId={}, exchange={}, routingKey={}",
                messageId, exchange, routingKey);
    }

    public void handleDeadLetter(String reason, String originalQueue, int bodyLength) {
        handleDeadLetter(null, reason, originalQueue, bodyLength);
    }

    /**
     * 带 messageId 的死信落库，可把本地消息表对应记录标为 DEAD。
     */
    public void handleDeadLetter(String messageId, String reason, String originalQueue, int bodyLength) {
        if (StringUtils.hasText(messageId)) {
            mqMessageService.markDead(messageId, reason);
        }
        notifyRecordService.saveAlarm("死信消息告警",
                trim("messageId=" + messageId + ", originalQueue=" + originalQueue
                        + ", bodyLength=" + bodyLength + ", reason=" + reason));
        log.error("告警-死信消息: messageId={}, reason={}, originalQueue={}, bodyLength={}",
                messageId, reason, originalQueue, bodyLength);
    }

    private static String trim(String content) {
        return content.length() > MAX_CONTENT_LENGTH ? content.substring(0, MAX_CONTENT_LENGTH) : content;
    }
}
