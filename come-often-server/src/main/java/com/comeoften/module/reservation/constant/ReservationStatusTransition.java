package com.comeoften.module.reservation.constant;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 预约状态迁移表。合法迁移集中在这里，Service 不散落 if 判断；
 * 真正的并发判据仍是带旧状态条件的 UPDATE。
 */
public final class ReservationStatusTransition {

    private static final Map<ReservationStatus, Set<ReservationStatus>> ALLOWED;

    static {
        Map<ReservationStatus, Set<ReservationStatus>> allowed = new EnumMap<>(ReservationStatus.class);
        allowed.put(ReservationStatus.PENDING, EnumSet.of(ReservationStatus.CONFIRMED,
                ReservationStatus.CHECKED_IN, ReservationStatus.NO_SHOW, ReservationStatus.CANCELLED));
        allowed.put(ReservationStatus.CONFIRMED, EnumSet.of(ReservationStatus.CHECKED_IN,
                ReservationStatus.NO_SHOW, ReservationStatus.CANCELLED));
        allowed.put(ReservationStatus.CHECKED_IN, Collections.emptySet());
        allowed.put(ReservationStatus.NO_SHOW, Collections.emptySet());
        allowed.put(ReservationStatus.CANCELLED, Collections.emptySet());
        ALLOWED = Collections.unmodifiableMap(allowed);
    }

    private ReservationStatusTransition() {
    }

    public static boolean canTransit(String fromCode, String toCode) {
        ReservationStatus from = ReservationStatus.of(fromCode);
        ReservationStatus to = ReservationStatus.of(toCode);
        if (from == null || to == null) {
            return false;
        }
        return ALLOWED.getOrDefault(from, Collections.emptySet()).contains(to);
    }

    /**
     * 前置校验，快速失败并给出业务码；并发安全仍由 SQL 条件更新保证。
     */
    public static void require(String fromCode, String toCode) {
        if (!canTransit(fromCode, toCode)) {
            throw new BizException(ErrorCode.RESERVATION_STATUS_INVALID);
        }
    }
}
