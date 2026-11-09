package com.comeoften.mq.service;

import com.comeoften.common.PageResult;
import com.comeoften.mq.entity.MqMessage;
import com.comeoften.mq.message.MqEnvelope;
import com.comeoften.mq.vo.MqMessageVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地消息表：投递不丢的第一道保障。业务事务内写 PENDING，事务提交后投递。
 */
public interface MqMessageService {

    /**
     * 在业务事务内登记 PENDING 消息，messageId 重复时忽略（幂等）。
     */
    void record(MqEnvelope envelope);

    MqEnvelope load(String messageId);

    void markSent(String messageId);

    /**
     * confirm 未到达或本地投递异常时重置为待重发，等待补偿任务扫描。
     */
    void markPending(String messageId, String cause, LocalDateTime nextRetryTime);

    void incrementRetry(String messageId, LocalDateTime nextRetryTime);

    void markFailed(String messageId, String cause);

    void markDead(String messageId, String cause);

    List<MqMessage> listPendingForRetry(LocalDateTime now, int limit);

    PageResult<MqMessageVO> pageForAdmin(Long current, Long size, String status);
}
