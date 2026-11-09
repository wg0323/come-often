package com.comeoften.integration.notify.impl;

import com.comeoften.integration.notify.NotifyCommand;
import com.comeoften.integration.notify.NotifySendResult;
import com.comeoften.integration.notify.NotifySender;
import com.comeoften.mq.entity.MessageRecord;
import com.comeoften.mq.mapper.MessageRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 默认通知通道：落一条 message_record（channel=LOG，send_status=1）并打日志。
 * 与 M3 起的行为完全一致，只是从 NotifyRecordServiceImpl 里抽成可替换的实现。
 */
@Slf4j
@RequiredArgsConstructor
public class LogNotifySender implements NotifySender {

    private static final int SEND_STATUS_DONE = 1;

    private final MessageRecordMapper messageRecordMapper;

    @Override
    public NotifySendResult send(NotifyCommand command) {
        MessageRecord record = new MessageRecord();
        record.setMemberId(command.getMemberId());
        record.setStoreId(command.getStoreId());
        record.setMessageType(command.getMessageType());
        record.setTitle(command.getTitle());
        record.setContent(command.getContent());
        record.setChannel(NotifySendResult.CHANNEL_LOG);
        record.setSendStatus(SEND_STATUS_DONE);
        record.setSendTime(LocalDateTime.now());
        messageRecordMapper.insert(record);
        // messageId 是排障主线（能对上 mq_message 与消费日志），只打日志不落库
        log.info("通知已落库(LOG 通道): type={}, title={}, messageId={}, memberId={}, storeId={}",
                command.getMessageType(), command.getTitle(), command.getMessageId(),
                command.getMemberId(), command.getStoreId());
        return NotifySendResult.degraded(NotifySendResult.CHANNEL_LOG);
    }
}
