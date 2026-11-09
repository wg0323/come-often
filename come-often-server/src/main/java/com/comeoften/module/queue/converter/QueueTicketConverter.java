package com.comeoften.module.queue.converter;

import com.comeoften.module.queue.constant.QueueTicketStatus;
import com.comeoften.module.queue.entity.QueueTicket;
import com.comeoften.module.queue.vo.QueueTicketVO;

/**
 * 排队号对象转换。状态文案与展示数据统一在这里派生。
 */
public final class QueueTicketConverter {

    private QueueTicketConverter() {
    }

    public static QueueTicketVO toVO(QueueTicket entity, Long waitingCount, String memberPhone) {
        QueueTicketVO vo = new QueueTicketVO();
        vo.setTicketNo(entity.getTicketNo());
        vo.setPartySize(entity.getPartySize());
        vo.setStatus(entity.getStatus());
        vo.setStatusText(QueueTicketStatus.describe(entity.getStatus()));
        vo.setWaitingCount(waitingCount == null ? 0L : waitingCount);
        vo.setCreateTime(entity.getCreateTime());
        vo.setCalledTime(entity.getCalledTime());
        vo.setBizDate(entity.getBizDate());
        vo.setMemberPhone(memberPhone == null ? "" : memberPhone);
        return vo;
    }
}
