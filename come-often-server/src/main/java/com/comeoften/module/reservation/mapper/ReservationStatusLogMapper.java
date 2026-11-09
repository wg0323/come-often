package com.comeoften.module.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.comeoften.module.reservation.entity.ReservationStatusLog;

/**
 * 预约状态日志 Mapper。每次状态迁移在同一事务内写一条，供后台详情与排障回溯。
 */
public interface ReservationStatusLogMapper extends BaseMapper<ReservationStatusLog> {
}
