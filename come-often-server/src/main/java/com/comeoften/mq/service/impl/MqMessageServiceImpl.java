package com.comeoften.mq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comeoften.common.PageResult;
import com.comeoften.mq.entity.MqMessage;
import com.comeoften.mq.mapper.MqMessageMapper;
import com.comeoften.mq.message.MqEnvelope;
import com.comeoften.mq.service.MqMessageService;
import com.comeoften.mq.vo.MqMessageVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地消息表实现。状态流转：PENDING(登记) → SENT(confirm 成功) / FAILED(重发耗尽) / DEAD(死信)。
 * 日志只记录 messageId 与元信息，不打印消息体原文。
 */
@Slf4j
@Service
public class MqMessageServiceImpl implements MqMessageService {

    private static final String STATUS_PENDING = "PENDING";

    private static final String STATUS_SENT = "SENT";

    private static final String STATUS_FAILED = "FAILED";

    private static final String STATUS_DEAD = "DEAD";

    private static final int MAX_CAUSE_LENGTH = 480;

    private final MqMessageMapper mqMessageMapper;

    private final ObjectMapper objectMapper;

    private final long pendingTimeoutSeconds;

    public MqMessageServiceImpl(MqMessageMapper mqMessageMapper, ObjectMapper objectMapper,
                                @Value("${comeoften.mq.pending-timeout-seconds:60}") long pendingTimeoutSeconds) {
        this.mqMessageMapper = mqMessageMapper;
        this.objectMapper = objectMapper;
        this.pendingTimeoutSeconds = pendingTimeoutSeconds;
    }

    @Override
    public void record(MqEnvelope envelope) {
        MqMessage record = new MqMessage();
        record.setMessageId(envelope.getMessageId());
        record.setExchangeName(envelope.getExchangeName());
        record.setRoutingKey(envelope.getRoutingKey());
        record.setPayload(toJson(envelope));
        record.setStatus(STATUS_PENDING);
        record.setRetryCount(0);
        record.setNextRetryTime(LocalDateTime.now().plusSeconds(pendingTimeoutSeconds));
        record.setErrorCause("");
        try {
            mqMessageMapper.insert(record);
        } catch (DuplicateKeyException e) {
            log.warn("本地消息已存在，跳过重复登记: messageId={}", envelope.getMessageId());
        }
    }

    @Override
    public MqEnvelope load(String messageId) {
        MqMessage record = find(messageId);
        if (record == null || !StringUtils.hasText(record.getPayload())) {
            return null;
        }
        try {
            return objectMapper.readValue(record.getPayload(), MqEnvelope.class);
        } catch (JsonProcessingException e) {
            log.error("本地消息体无法反序列化: messageId={}", messageId, e);
            return null;
        }
    }

    @Override
    public void markSent(String messageId) {
        // 只允许 PENDING→SENT，避免迟到的 confirm 把路由失败(FAILED)/死信(DEAD)覆盖成成功
        updateStatus(messageId, STATUS_SENT, "", STATUS_PENDING);
        log.info("消息投递已确认: messageId={}", messageId);
    }

    @Override
    public void markPending(String messageId, String cause, LocalDateTime nextRetryTime) {
        mqMessageMapper.update(null, new LambdaUpdateWrapper<MqMessage>()
                .eq(MqMessage::getMessageId, messageId)
                .eq(MqMessage::getStatus, STATUS_PENDING)
                .set(MqMessage::getStatus, STATUS_PENDING)
                .set(MqMessage::getErrorCause, trimCause(cause))
                .set(MqMessage::getNextRetryTime, nextRetryTime));
    }

    @Override
    public void incrementRetry(String messageId, LocalDateTime nextRetryTime) {
        mqMessageMapper.update(null, new LambdaUpdateWrapper<MqMessage>()
                .eq(MqMessage::getMessageId, messageId)
                .setSql("retry_count = retry_count + 1")
                .set(MqMessage::getNextRetryTime, nextRetryTime));
    }

    @Override
    public void markFailed(String messageId, String cause) {
        updateStatus(messageId, STATUS_FAILED, trimCause(cause), STATUS_PENDING);
        log.error("消息最终投递失败，转人工告警: messageId={}, cause={}", messageId, cause);
    }

    @Override
    public void markDead(String messageId, String cause) {
        // 死信可以覆盖任何非终态，包括已 SENT 但消费失败的消息
        updateStatus(messageId, STATUS_DEAD, trimCause(cause), null);
    }

    @Override
    public List<MqMessage> listPendingForRetry(LocalDateTime now, int limit) {
        int size = Math.max(1, Math.min(limit, 200));
        return mqMessageMapper.selectList(new LambdaQueryWrapper<MqMessage>()
                .eq(MqMessage::getStatus, STATUS_PENDING)
                .le(MqMessage::getNextRetryTime, now)
                .orderByAsc(MqMessage::getNextRetryTime)
                .last("LIMIT " + size));
    }

    @Override
    public PageResult<MqMessageVO> pageForAdmin(Long current, Long size, String status) {
        Page<MqMessage> page = new Page<>(current, size);
        IPage<MqMessage> result = mqMessageMapper.selectPage(page,
                new LambdaQueryWrapper<MqMessage>()
                        .eq(StringUtils.hasText(status), MqMessage::getStatus, status)
                        .orderByDesc(MqMessage::getCreateTime));
        List<MqMessageVO> records = new ArrayList<>();
        for (MqMessage entity : result.getRecords()) {
            MqMessageVO vo = new MqMessageVO();
            BeanUtils.copyProperties(entity, vo);
            records.add(vo);
        }
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    private MqMessage find(String messageId) {
        if (!StringUtils.hasText(messageId)) {
            return null;
        }
        return mqMessageMapper.selectOne(new LambdaQueryWrapper<MqMessage>()
                .eq(MqMessage::getMessageId, messageId));
    }

    private void updateStatus(String messageId, String status, String cause, String expectedStatus) {
        if (!StringUtils.hasText(messageId)) {
            return;
        }
        LambdaUpdateWrapper<MqMessage> wrapper = new LambdaUpdateWrapper<MqMessage>()
                .eq(MqMessage::getMessageId, messageId)
                .set(MqMessage::getStatus, status)
                .set(MqMessage::getErrorCause, cause);
        if (expectedStatus != null) {
            wrapper.eq(MqMessage::getStatus, expectedStatus);
        }
        mqMessageMapper.update(null, wrapper);
    }

    private String toJson(MqEnvelope envelope) {
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("本地消息体序列化失败", e);
        }
    }

    private static String trimCause(String cause) {
        String value = cause == null ? "" : cause;
        return value.length() > MAX_CAUSE_LENGTH ? value.substring(0, MAX_CAUSE_LENGTH) : value;
    }
}
