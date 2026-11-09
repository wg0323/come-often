package com.comeoften.module.reservation.service.impl;

import com.comeoften.config.CouponProperties;
import com.comeoften.config.ReservationProperties;
import com.comeoften.module.auth.mapper.MemberMapper;
import com.comeoften.module.reservation.constant.ReservationStatus;
import com.comeoften.module.reservation.entity.Reservation;
import com.comeoften.module.reservation.entity.ReservationSlot;
import com.comeoften.module.reservation.mapper.ReservationMapper;
import com.comeoften.module.reservation.mapper.ReservationSlotMapper;
import com.comeoften.module.reservation.mapper.ReservationStatusLogMapper;
import com.comeoften.module.reservation.service.ReservationSlotService;
import com.comeoften.module.store.mapper.StoreMapper;
import com.comeoften.module.store.mapper.StoreTableMapper;
import com.comeoften.mq.MessageProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 超时未签到的三条关键分支：正常落库释放、提前到达被拒、改期后的旧消息被丢弃。
 * 这三条对应独立审查发现的幂等键与时间守卫缺陷，作为回归护栏。
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private ReservationSlotMapper reservationSlotMapper;

    @Mock
    private ReservationStatusLogMapper reservationStatusLogMapper;

    @Mock
    private ReservationSlotService reservationSlotService;

    @Mock
    private StoreMapper storeMapper;

    @Mock
    private StoreTableMapper storeTableMapper;

    @Mock
    private MemberMapper memberMapper;

    @Spy
    private ReservationProperties reservationProperties = new ReservationProperties();

    @Mock
    private CouponProperties couponProperties;

    @Mock
    private MessageProducer messageProducer;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    @Test
    void should_mark_no_show_and_release_slot_when_timeout_arrives() {
        LocalDateTime reserveAt = LocalDateTime.now().minusMinutes(30).withSecond(0).withNano(0);
        Reservation entity = reservation(reserveAt, ReservationStatus.CONFIRMED.name());
        when(reservationMapper.selectByReserveNo("RE-TIMEOUT")).thenReturn(entity);
        when(reservationMapper.markNoShow(entity.getId())).thenReturn(1);
        ReservationSlot slot = new ReservationSlot();
        slot.setId(99L);
        when(reservationSlotService.findSlot(entity.getStoreId(), entity.getReserveDate(),
                entity.getTimeSlot(), entity.getTableType())).thenReturn(slot);
        when(reservationSlotService.releaseInTransaction(99L)).thenReturn(true);

        boolean marked = reservationService.markNoShow("RE-TIMEOUT", toMillis(reserveAt));

        assertThat(marked).isTrue();
        verify(reservationMapper).markNoShow(entity.getId());
        verify(reservationSlotService).releaseInTransaction(99L);
        verify(reservationStatusLogMapper).insert(any());
    }

    @Test
    void should_reject_timeout_that_arrives_before_no_show_window() {
        LocalDateTime reserveAt = LocalDateTime.now().plusHours(2).withSecond(0).withNano(0);
        Reservation entity = reservation(reserveAt, ReservationStatus.CONFIRMED.name());
        when(reservationMapper.selectByReserveNo("RE-FUTURE")).thenReturn(entity);

        boolean marked = reservationService.markNoShow("RE-FUTURE", toMillis(reserveAt));

        assertThat(marked).isFalse();
        verify(reservationMapper, never()).markNoShow(anyLong());
        verify(reservationSlotService, never()).releaseInTransaction(anyLong());
    }

    @Test
    void should_ignore_timeout_message_when_reservation_was_rescheduled() {
        LocalDateTime currentReserveAt = LocalDateTime.now().plusHours(3).withSecond(0).withNano(0);
        Reservation entity = reservation(currentReserveAt, ReservationStatus.CONFIRMED.name());
        when(reservationMapper.selectByReserveNo("RE-MOVED")).thenReturn(entity);
        long staleMillis = toMillis(currentReserveAt.minusHours(3));

        boolean marked = reservationService.markNoShow("RE-MOVED", staleMillis);

        assertThat(marked).isFalse();
        verify(reservationMapper, never()).markNoShow(anyLong());
    }

    @Test
    void should_not_release_when_status_changed_concurrently() {
        LocalDateTime reserveAt = LocalDateTime.now().minusMinutes(30).withSecond(0).withNano(0);
        Reservation entity = reservation(reserveAt, ReservationStatus.CONFIRMED.name());
        when(reservationMapper.selectByReserveNo("RE-RACE")).thenReturn(entity);
        when(reservationMapper.markNoShow(entity.getId())).thenReturn(0);

        boolean marked = reservationService.markNoShow("RE-RACE", toMillis(reserveAt));

        assertThat(marked).isFalse();
        verify(reservationSlotService, never()).releaseInTransaction(anyLong());
    }

    private static Reservation reservation(LocalDateTime reserveAt, String status) {
        Reservation entity = new Reservation();
        entity.setId(1001L);
        entity.setReserveNo("RE-TIMEOUT");
        entity.setStoreId(1L);
        entity.setMemberId(2L);
        entity.setReserveDate(reserveAt.toLocalDate());
        entity.setTimeSlot(reserveAt.format(TIME_FORMATTER));
        entity.setTableType("HALL_CARD");
        entity.setPeopleCount(2);
        entity.setStatus(status);
        return entity;
    }

    private static long toMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
