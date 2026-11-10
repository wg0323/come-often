package com.comeoften.module.order.service.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.PageResult;
import com.comeoften.module.order.constant.OperatorType;
import com.comeoften.module.order.constant.VerifyChannel;
import com.comeoften.module.order.entity.OrderVerifyRecord;
import com.comeoften.module.order.entity.TradeOrder;
import com.comeoften.module.order.mapper.OrderVerifyRecordMapper;
import com.comeoften.module.order.service.OrderVerifyService;
import com.comeoften.module.order.util.PickupCodeGenerator;
import com.comeoften.module.order.vo.OrderVerifyRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 核销审计实现。只认「订单推进到 COMPLETED」这一个写入点，通道由操作方派生。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderVerifyServiceImpl implements OrderVerifyService {

    private static final long DEFAULT_PAGE_SIZE = 10L;

    private static final long MAX_PAGE_SIZE = 50L;

    private static final String BIZ_SUFFIX_PICKUP = "#PICKUP";

    private static final String BIZ_SUFFIX_BUNDLE = "#BUNDLE";

    private final OrderVerifyRecordMapper orderVerifyRecordMapper;

    @Override
    public void record(TradeOrder order, OperatorType operatorType, Long operatorId) {
        String code = order.getPickupCode();
        if (!StringUtils.hasText(code) || !PickupCodeGenerator.matches(code)) {
            // 审计不能反向阻断订单完成：历史 / 导入订单的 pickup_code 允许为 NULL 或不合法，
            // 抛异常会让 CAS 已改完状态的完成流程整体回滚，订单永久停在 READY。
            // 这里只记 warn 并跳过审计，状态推进照常提交。30088 因此成为防御性保留码。
            log.warn("取餐码异常，跳过核销审计: orderNo={}, pickupCode={}",
                    order.getOrderNo(), code);
            return;
        }
        String bizId = order.getOrderNo()
                + (order.getBundleId() == null ? BIZ_SUFFIX_PICKUP : BIZ_SUFFIX_BUNDLE);
        OrderVerifyRecord record = new OrderVerifyRecord();
        record.setOrderId(order.getId());
        record.setStoreId(order.getStoreId());
        record.setVerifyCode(code);
        record.setVerifyChannel(VerifyChannel.of(operatorType));
        record.setOperatorType(operatorType == null ? OperatorType.SYSTEM.name() : operatorType.name());
        record.setOperatorId(operatorType == OperatorType.MEMBER ? null : operatorId);
        record.setVerifyTime(LocalDateTime.now());
        record.setBizId(bizId);
        record.setRemark(order.getBundleId() == null ? "取餐码核销" : "团购套餐核销");
        try {
            orderVerifyRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发重复推进 / 重复扫码：订单已经核销过，不能再插第二条
            throw new BizException(ErrorCode.ORDER_ALREADY_VERIFIED);
        }
        log.info("订单核销已记录: orderNo={}, bizId={}, channel={}",
                order.getOrderNo(), bizId, record.getVerifyChannel());
    }

    @Override
    public PageResult<OrderVerifyRecordVO> pageForMember(Long memberId, Long storeId,
                                                          Long current, Long size) {
        long pageNo = current == null || current < 1 ? 1L : current;
        long pageSize = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        Long total = orderVerifyRecordMapper.countForMember(memberId, storeId);
        long count = total == null ? 0L : total;
        if (count == 0L) {
            return PageResult.of(0L, pageNo, pageSize, Collections.emptyList());
        }
        List<OrderVerifyRecordVO> records = orderVerifyRecordMapper.pageForMember(memberId, storeId,
                (pageNo - 1L) * pageSize, pageSize);
        decorate(records);
        return PageResult.of(count, pageNo, pageSize, records);
    }

    @Override
    public OrderVerifyRecordVO detailForMember(Long memberId, Long storeId, Long recordId) {
        if (recordId == null) {
            throw new BizException(ErrorCode.ORDER_VERIFY_RECORD_NOT_FOUND);
        }
        OrderVerifyRecordVO vo = orderVerifyRecordMapper.selectOwned(recordId, memberId, storeId);
        if (vo == null) {
            throw new BizException(ErrorCode.ORDER_VERIFY_RECORD_NOT_FOUND);
        }
        decorate(Collections.singletonList(vo));
        return vo;
    }

    private static void decorate(List<OrderVerifyRecordVO> records) {
        for (OrderVerifyRecordVO vo : records) {
            vo.setChannelText(VerifyChannel.describe(vo.getVerifyChannel()));
        }
    }
}
