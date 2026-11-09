package com.comeoften.module.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.module.merchant.service.MerchantWorkbenchService;
import com.comeoften.module.merchant.vo.WorkbenchSummaryVO;
import com.comeoften.module.order.constant.OrderStatus;
import com.comeoften.module.order.entity.TradeOrder;
import com.comeoften.module.order.mapper.TradeOrderMapper;
import com.comeoften.module.reservation.constant.ReservationStatus;
import com.comeoften.module.reservation.entity.Reservation;
import com.comeoften.module.reservation.mapper.ReservationMapper;
import com.comeoften.module.store.entity.Store;
import com.comeoften.module.store.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工作台概览实现。全部为 count / sum 聚合，不加载明细。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantWorkbenchServiceImpl implements MerchantWorkbenchService {

    private final TradeOrderMapper tradeOrderMapper;

    private final ReservationMapper reservationMapper;

    private final StoreMapper storeMapper;

    @Override
    public WorkbenchSummaryVO summary(Long storeId) {
        WorkbenchSummaryVO vo = new WorkbenchSummaryVO();
        Store store = storeMapper.selectById(storeId);
        vo.setStoreName(store == null ? "" : store.getStoreName());
        vo.setPendingAcceptCount(countOrders(storeId, OrderStatus.PAID));
        vo.setCookingCount(countOrders(storeId, OrderStatus.ACCEPTED));
        vo.setReadyCount(countOrders(storeId, OrderStatus.READY));
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1L).atStartOfDay();
        BigDecimal revenue = tradeOrderMapper.sumStorePaidAmount(storeId, from, to);
        vo.setTodayRevenue(revenue == null ? BigDecimal.ZERO : revenue);
        vo.setTodayReservationCount(reservationMapper.selectCount(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getStoreId, storeId)
                .eq(Reservation::getReserveDate, today)
                .ne(Reservation::getStatus, ReservationStatus.CANCELLED.name())));
        log.debug("工作台概览查询完成: storeId={}, pending={}, cooking={}, ready={}",
                storeId, vo.getPendingAcceptCount(), vo.getCookingCount(), vo.getReadyCount());
        return vo;
    }

    private Long countOrders(Long storeId, OrderStatus status) {
        return tradeOrderMapper.selectCount(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getStoreId, storeId)
                .eq(TradeOrder::getStatus, status.name()));
    }
}
