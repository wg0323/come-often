package com.comeoften.module.queue.service;

import com.comeoften.common.PageResult;
import com.comeoften.module.queue.vo.QueueTicketVO;

/**
 * 现场排队叫号：顾客取号 / 查号 / 取消，商家与后台叫号 / 入座。
 */
public interface QueueTicketService {

    /**
     * 取号。同一会员同一营业日已有等待中或已叫号的号时抛 30059。
     */
    QueueTicketVO take(Long memberId, Long storeId, Integer partySize);

    /**
     * 当前会员今日最新有效号，没有返回 null。
     */
    QueueTicketVO current(Long memberId, Long storeId);

    /**
     * 顾客取消自己的号。
     */
    QueueTicketVO cancel(Long memberId, Long storeId, String ticketNo);

    /**
     * 当天排队号分页。status 为空表示全部。
     */
    PageResult<QueueTicketVO> page(Long storeId, String status, Long current, Long size);

    /**
     * 叫号：WAITING → CALLED。
     */
    QueueTicketVO call(Long storeId, String ticketNo);

    /**
     * 入座：CALLED → SEATED。
     */
    QueueTicketVO seat(Long storeId, String ticketNo);
}
