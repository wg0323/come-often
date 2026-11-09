package com.comeoften.module.reservation.converter;

import com.comeoften.module.auth.entity.Member;
import com.comeoften.module.reservation.constant.ReservationStatus;
import com.comeoften.module.reservation.constant.ReservationTableType;
import com.comeoften.module.reservation.entity.Reservation;
import com.comeoften.module.reservation.entity.ReservationSlot;
import com.comeoften.module.reservation.entity.ReservationStatusLog;
import com.comeoften.module.reservation.vo.ReservationSlotVO;
import com.comeoften.module.reservation.vo.ReservationStatusLogVO;
import com.comeoften.module.reservation.vo.ReservationVO;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 预约对象转换。状态文案、预约时刻与可取消/可签到判断都在这里算，页面与后台只展示。
 */
public final class ReservationConverter {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private ReservationConverter() {
    }

    public static ReservationVO toVO(Reservation entity, String storeName, Member member,
                                     long cancelLimitHours, LocalDateTime now) {
        ReservationVO vo = new ReservationVO();
        vo.setId(entity.getId());
        vo.setReserveNo(entity.getReserveNo());
        vo.setStoreId(entity.getStoreId());
        vo.setStoreName(storeName);
        vo.setReserveDate(entity.getReserveDate());
        vo.setTimeSlot(entity.getTimeSlot());
        vo.setTableType(entity.getTableType());
        vo.setTableTypeText(ReservationTableType.describe(entity.getTableType()));
        vo.setPeopleCount(entity.getPeopleCount());
        vo.setContactName(entity.getContactName());
        vo.setContactPhone(entity.getContactPhone());
        vo.setRemark(entity.getRemark());
        vo.setStatus(entity.getStatus());
        vo.setStatusText(ReservationStatus.describe(entity.getStatus()));
        vo.setTableId(entity.getTableId());
        vo.setCheckinUserId(entity.getCheckinUserId());
        vo.setCheckinTime(entity.getCheckinTime());
        vo.setCreateTime(entity.getCreateTime());
        if (member != null) {
            vo.setMemberNickname(member.getNickname());
            vo.setMemberPhone(member.getPhone());
        }
        LocalDateTime reserveAt = reserveAt(entity);
        vo.setReserveAt(reserveAt);
        boolean active = ReservationStatus.active(entity.getStatus());
        vo.setCanCancel(active && reserveAt != null && now.isBefore(reserveAt.minusHours(cancelLimitHours)));
        vo.setCanCheckin(active);
        return vo;
    }

    public static ReservationSlotVO toSlotVO(ReservationSlot slot) {
        ReservationSlotVO vo = new ReservationSlotVO();
        int capacity = slot.getCapacity() == null ? 0 : slot.getCapacity();
        int booked = slot.getBooked() == null ? 0 : slot.getBooked();
        vo.setSlotId(slot.getId());
        vo.setReserveDate(slot.getReserveDate());
        vo.setTimeSlot(slot.getTimeSlot());
        vo.setTableType(slot.getTableType());
        vo.setTableTypeText(ReservationTableType.describe(slot.getTableType()));
        vo.setCapacity(capacity);
        vo.setBooked(booked);
        vo.setRemaining(Math.max(0, capacity - booked));
        vo.setFull(booked >= capacity);
        return vo;
    }

    public static ReservationStatusLogVO toLogVO(ReservationStatusLog entity) {
        ReservationStatusLogVO vo = new ReservationStatusLogVO();
        vo.setFromStatus(entity.getFromStatus());
        vo.setFromStatusText(entity.getFromStatus() == null || entity.getFromStatus().isEmpty()
                ? "新建" : ReservationStatus.describe(entity.getFromStatus()));
        vo.setToStatus(entity.getToStatus());
        vo.setToStatusText(ReservationStatus.describe(entity.getToStatus()));
        vo.setOperatorId(entity.getOperatorId());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    /**
     * 预约时刻 = 日期 + 时段。时段是 M7 唯一的时间口径，不接受前端传日期时间。
     */
    public static LocalDateTime reserveAt(Reservation entity) {
        if (entity.getReserveDate() == null || entity.getTimeSlot() == null) {
            return null;
        }
        return LocalDateTime.of(entity.getReserveDate(), LocalTime.parse(entity.getTimeSlot(), TIME_FORMATTER));
    }
}
