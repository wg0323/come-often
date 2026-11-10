package com.comeoften.module.reservation.service;

import com.comeoften.common.PageResult;
import com.comeoften.module.reservation.dto.ReservationCheckinDTO;
import com.comeoften.module.reservation.dto.ReservationCreateDTO;
import com.comeoften.module.reservation.dto.ReservationRescheduleDTO;
import com.comeoften.module.reservation.vo.ReservationCalendarDayVO;
import com.comeoften.module.reservation.vo.ReservationCodeVO;
import com.comeoften.module.reservation.vo.ReservationMetaVO;
import com.comeoften.module.reservation.vo.ReservationRoomVO;
import com.comeoften.module.reservation.vo.ReservationSlotVO;
import com.comeoften.module.reservation.vo.ReservationTableBoardVO;
import com.comeoften.module.reservation.vo.ReservationVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 预约订座。创建/改期/取消都遵循「Redis 预占 → DB 事务 → 失败回补 Redis」的固定顺序；
 * 状态写一律带旧状态条件，超时与提醒走延迟消息。
 */
public interface ReservationService {

    /**
     * 预约页元数据：门店、可预约日期、时段与桌型规则。
     */
    ReservationMetaVO meta(Long storeId);

    /**
     * 指定日期的时段余量。展示口径读 DB，Redis 只作扣减前置判定。
     *
     * @param includePast 是否包含当天已经过去的时段；后台看板需要，顾客端不需要
     */
    List<ReservationSlotVO> slots(Long storeId, LocalDate reserveDate, boolean includePast);

    /**
     * 指定日期时段仍可锁定的包间。只返回标了 room_type 的桌台，已占用的自动排除。
     */
    List<ReservationRoomVO> rooms(Long storeId, LocalDate reserveDate, String timeSlot, Integer peopleCount);

    /**
     * 创建预约。时段已满抛 30041，时段未开放抛 30042，人数与桌型不匹配抛 30044；
     * 传 tableId 时锁具体包间，包间类型不合法抛 30098。
     */
    ReservationVO create(Long memberId, Long storeId, ReservationCreateDTO dto);

    /**
     * 改期。先占新时段再放旧时段，任一失败整体回滚。
     */
    ReservationVO reschedule(Long memberId, String reserveNo, ReservationRescheduleDTO dto);

    /**
     * 取消。距预约不足 2 小时抛 30045。
     */
    ReservationVO cancel(Long memberId, String reserveNo);

    PageResult<ReservationVO> pageForMember(Long memberId, String tab, Long current, Long size);

    ReservationVO detailForMember(Long memberId, String reserveNo);

    /**
     * 动态签到码，30 秒一个窗口，带 R 前缀供扫码分流。
     */
    ReservationCodeVO checkinCode(Long memberId, String reserveNo);

    PageResult<ReservationVO> adminPage(Long storeId, LocalDate reserveDate, String status, String keyword,
                                        Long current, Long size);

    ReservationVO adminDetail(Long storeId, String reserveNo);

    ReservationVO confirm(Long storeId, Long operatorId, String reserveNo);

    ReservationVO reject(Long storeId, Long operatorId, String reserveNo, String remark);

    /**
     * 后台按预约单号签到，可同时绑定具体桌台。
     */
    ReservationVO checkin(Long storeId, Long operatorId, String reserveNo, Long tableId);

    /**
     * 扫码签到：校验 R 前缀、签名与时间窗。
     */
    ReservationVO checkinByCode(Long storeId, Long operatorId, ReservationCheckinDTO dto);

    List<ReservationCalendarDayVO> calendar(Long storeId, LocalDate startDate, LocalDate endDate);

    ReservationTableBoardVO tableBoard(Long storeId, LocalDate reserveDate);

    /**
     * 超时未签到。expectedReserveAtMillis 与当前预约时刻不一致（已改期）时直接忽略。
     *
     * @return 是否真的置为 NO_SHOW
     */
    boolean markNoShow(String reserveNo, long expectedReserveAtMillis);

    /**
     * 提醒消息是否仍需要发送：预约存在、仍有效且未改期。
     */
    boolean shouldRemind(String reserveNo, long expectedReserveAtMillis);

    /**
     * 运维与验收补发：kind=timeout 投超时消息，kind=remind 投提醒消息。
     */
    void sendProbe(Long storeId, String reserveNo, String kind, long delaySeconds);
}
