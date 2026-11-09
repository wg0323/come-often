package com.comeoften.module.merchant.service.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.PageResult;
import com.comeoften.module.auth.service.PermissionService;
import com.comeoften.module.merchant.service.MerchantOrderService;
import com.comeoften.module.order.constant.OperatorType;
import com.comeoften.module.order.constant.OrderStatus;
import com.comeoften.module.order.service.OrderService;
import com.comeoften.module.order.vo.OrderSummaryVO;
import com.comeoften.module.order.vo.OrderVO;
import com.comeoften.module.payment.dto.RefundApplyDTO;
import com.comeoften.module.payment.service.RefundService;
import com.comeoften.module.payment.vo.RefundVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 商家端订单看板实现。状态推进、日志、MQ 事件全部复用 OrderService.transition，
 * 这里只做「商家端只允许三步」的白名单与退款入口包装。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantOrderServiceImpl implements MerchantOrderService {

    /** 商家端可推进的目标状态，取消与退款审核不在看板放开。 */
    private static final Set<OrderStatus> ALLOWED_TARGETS = Collections.unmodifiableSet(
            EnumSet.of(OrderStatus.ACCEPTED, OrderStatus.READY, OrderStatus.COMPLETED));

    /**
     * 可发起退单的角色。M1 给服务员授予了 order:accept（接单），但退款在制作前会自动审核通过且不可逆，
     * 只靠权限点会让服务员比 PC 后台（order:manage）权限更松，因此在服务层再收一道角色白名单。
     */
    private static final Set<String> REFUND_ROLES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("SUPER_ADMIN", "STORE_MANAGER")));

    private static final long MERCHANT_PAGE_MAX_SIZE = 50L;

    private static final long MERCHANT_PAGE_DEFAULT_SIZE = 20L;

    private final OrderService orderService;

    private final RefundService refundService;

    private final PermissionService permissionService;

    @Override
    public PageResult<OrderSummaryVO> page(Long storeId, Long current, Long size, String status) {
        long pageCurrent = Math.max(1L, current == null ? 1L : current);
        long pageSize = Math.min(Math.max(1L, size == null ? MERCHANT_PAGE_DEFAULT_SIZE : size),
                MERCHANT_PAGE_MAX_SIZE);
        return orderService.pageForAdmin(storeId, pageCurrent, pageSize, status, null);
    }

    @Override
    public OrderVO detail(Long storeId, String orderNo) {
        return orderService.detailForAdmin(storeId, orderNo);
    }

    @Override
    public OrderVO advance(Long storeId, String orderNo, String toStatus, String remark, Long operatorId) {
        OrderStatus target = OrderStatus.of(normalizeStatus(toStatus));
        if (target == null || !ALLOWED_TARGETS.contains(target)) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID,
                    "商家端只支持接单、出餐与确认取餐：" + toStatus);
        }
        return orderService.transition(storeId, orderNo, target.name(), remark,
                OperatorType.MERCHANT, operatorId);
    }

    @Override
    public RefundVO refund(Long storeId, String orderNo, String reason, Long operatorId) {
        assertRefundRole(operatorId);
        RefundApplyDTO dto = new RefundApplyDTO();
        dto.setReason(reason);
        log.info("商家端发起退单: storeId={}, orderNo={}, operatorId={}", storeId, orderNo, operatorId);
        return refundService.applyByAdmin(storeId, orderNo, dto, operatorId);
    }

    private void assertRefundRole(Long operatorId) {
        boolean allowed = permissionService.listRoleCodes(operatorId).stream().anyMatch(REFUND_ROLES::contains);
        if (!allowed) {
            throw new BizException(ErrorCode.PERMISSION_DENIED, "仅店长或超级管理员可发起退单");
        }
    }

    private static String normalizeStatus(String toStatus) {
        if (!StringUtils.hasText(toStatus)) {
            return "";
        }
        return toStatus.trim().toUpperCase(Locale.ROOT);
    }
}
