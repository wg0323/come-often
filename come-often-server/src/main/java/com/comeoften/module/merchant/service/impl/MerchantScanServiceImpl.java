package com.comeoften.module.merchant.service.impl;

import com.comeoften.common.BizException;
import com.comeoften.module.coupon.constant.VerifyChannel;
import com.comeoften.module.coupon.dto.CouponVerifyDTO;
import com.comeoften.module.coupon.service.CouponVerifyService;
import com.comeoften.module.coupon.vo.CouponVerifyVO;
import com.comeoften.module.merchant.constant.ScanCodeType;
import com.comeoften.module.merchant.constant.ScanFailReason;
import com.comeoften.module.merchant.service.MerchantScanService;
import com.comeoften.module.merchant.vo.ScanResolveVO;
import com.comeoften.module.order.constant.OperatorType;
import com.comeoften.module.order.constant.OrderStatus;
import com.comeoften.module.order.constant.OrderType;
import com.comeoften.module.order.entity.TradeOrder;
import com.comeoften.module.order.mapper.TradeOrderMapper;
import com.comeoften.module.order.service.OrderService;
import com.comeoften.module.reservation.dto.ReservationCheckinDTO;
import com.comeoften.module.reservation.service.ReservationService;
import com.comeoften.module.reservation.vo.ReservationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 统一扫码实现。分流判据见 {@link ScanCodeType}：券 C + 16 位、预约 R + 单号 + 10 位签名、
 * 取餐 A001~G999（自提 A 段、到店核销 D 段起，字母区间由 {@code PickupCodeGenerator} 给出）。
 * 计划 3.12 写的「P → 取餐核销」在 M3 已按 3.3 落地为 A###/D###，不为了对齐文档再引入一个不存在的 P 前缀。
 *
 * <p><b>本类刻意不加 {@code @Transactional}：</b>下游券核销 / 签到 / 状态推进各自带事务，
 * 这里的 {@code catch (BizException)} 必须落在它们的事务代理之外，否则会变成「事务内吞异常 → 提交」。
 * 后续若要加审计或加锁，请包在下游 Service 里，不要给 {@code resolve()} 套事务。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantScanServiceImpl implements MerchantScanService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String BLANK_CHAR = " ";

    private final CouponVerifyService couponVerifyService;

    private final ReservationService reservationService;

    private final OrderService orderService;

    private final TradeOrderMapper tradeOrderMapper;

    @Override
    public ScanResolveVO resolve(Long storeId, Long operatorUserId, String rawCode) {
        String code = normalize(rawCode);
        ScanCodeType codeType = ScanCodeType.detect(code);
        log.info("商家端扫码: storeId={}, operatorUserId={}, codeType={}", storeId, operatorUserId, codeType);
        try {
            switch (codeType) {
                case COUPON:
                    return verifyCoupon(storeId, operatorUserId, code);
                case RESERVATION:
                    return checkinReservation(storeId, operatorUserId, code);
                case PICKUP:
                    return completePickup(storeId, operatorUserId, code);
                default:
                    return fail(codeType, ScanFailReason.CODE_UNRECOGNIZED, null);
            }
        } catch (BizException e) {
            ScanFailReason reason = ScanFailReason.of(e.getErrorCode());
            ScanResolveVO vo = fail(codeType, reason, e.getMessage());
            log.info("商家端扫码失败: storeId={}, codeType={}, reason={}, message={}",
                    storeId, codeType, reason, e.getMessage());
            return vo;
        } catch (RuntimeException e) {
            // 非业务异常也回统一结构，否则前端要处理两种响应体
            log.error("商家端扫码异常: storeId={}, codeType={}", storeId, codeType, e);
            return fail(codeType, ScanFailReason.SYSTEM_ERROR, null);
        }
    }

    private ScanResolveVO verifyCoupon(Long storeId, Long operatorUserId, String code) {
        CouponVerifyDTO dto = new CouponVerifyDTO();
        dto.setCode(code);
        CouponVerifyVO verify = couponVerifyService.verify(storeId, operatorUserId, dto, VerifyChannel.MERCHANT_SCAN);
        ScanResolveVO vo = success(ScanCodeType.COUPON, "券核销成功",
                String.format(Locale.ROOT, "「%s」已核销，抵扣 %s 元",
                        verify.getTemplateName(), plain(verify.getDiscountAmount())));
        vo.setBizNo(verify.getUserCouponId() == null ? null : String.valueOf(verify.getUserCouponId()));
        vo.setTemplateName(verify.getTemplateName());
        vo.setCouponTypeText(verify.getCouponTypeText());
        vo.setFaceValue(verify.getFaceValue());
        vo.setThresholdAmount(verify.getThresholdAmount());
        vo.setDiscountAmount(verify.getDiscountAmount());
        return vo;
    }

    private ScanResolveVO checkinReservation(Long storeId, Long operatorUserId, String code) {
        ReservationCheckinDTO dto = new ReservationCheckinDTO();
        dto.setCode(code);
        ReservationVO checkin = reservationService.checkinByCode(storeId, operatorUserId, dto);
        ScanResolveVO vo = success(ScanCodeType.RESERVATION, "签到成功",
                String.format(Locale.ROOT, "%s %s · %s · %s 位",
                        checkin.getReserveDate(), checkin.getTimeSlot(),
                        checkin.getTableTypeText(), checkin.getPeopleCount()));
        vo.setBizNo(checkin.getReserveNo());
        vo.setReserveDate(checkin.getReserveDate() == null ? null : checkin.getReserveDate().toString());
        vo.setTimeSlot(checkin.getTimeSlot());
        vo.setTableTypeText(checkin.getTableTypeText());
        vo.setContactName(checkin.getContactName());
        vo.setPeopleCount(checkin.getPeopleCount());
        vo.setCheckinTime(format(checkin.getCheckinTime()));
        if (checkin.getReserveAt() != null && checkin.getReserveAt().isAfter(LocalDateTime.now())) {
            vo.setNotice("提前签到：预约时间为 " + format(checkin.getReserveAt()));
        }
        return vo;
    }

    private ScanResolveVO completePickup(Long storeId, Long operatorUserId, String code) {
        TradeOrder order = findPickupOrder(storeId, code);
        if (order == null) {
            return fail(ScanCodeType.PICKUP, ScanFailReason.ORDER_NOT_FOUND, "取餐码不存在或不属于今日");
        }
        OrderStatus status = OrderStatus.of(order.getStatus());
        ScanFailReason notReady = notReadyReason(status);
        if (notReady != null) {
            return fail(ScanCodeType.PICKUP, notReady, null);
        }
        try {
            orderService.transition(storeId, order.getOrderNo(), OrderStatus.COMPLETED.name(),
                    "商家端扫码确认取餐", OperatorType.MERCHANT, operatorUserId);
        } catch (BizException e) {
            // 并发下第二个人扫码时状态已被改走，按最新状态给出可读原因
            TradeOrder latest = findPickupOrder(storeId, code);
            if (latest != null && OrderStatus.COMPLETED.name().equals(latest.getStatus())) {
                return fail(ScanCodeType.PICKUP, ScanFailReason.ORDER_COMPLETED, null);
            }
            return fail(ScanCodeType.PICKUP, ScanFailReason.ORDER_STATUS_INVALID, e.getMessage());
        }
        ScanResolveVO vo = success(ScanCodeType.PICKUP, "取餐完成",
                String.format(Locale.ROOT, "订单 %s（%s）已确认取餐", order.getOrderNo(), code));
        vo.setBizNo(order.getOrderNo());
        vo.setOrderNo(order.getOrderNo());
        vo.setPickupCode(code);
        OrderType type = OrderType.of(order.getOrderType());
        vo.setOrderTypeText(type == null ? "" : type.getDescription());
        vo.setOrderStatusText(OrderStatus.COMPLETED.getDescription());
        return vo;
    }

    /**
     * 取餐码反查订单。跨零点取餐时码属于前一个营业日，今天查不到再回退到昨天。
     */
    private TradeOrder findPickupOrder(Long storeId, String code) {
        LocalDate today = LocalDate.now();
        TradeOrder order = tradeOrderMapper.selectByPickupCode(storeId, today, code);
        if (order == null) {
            order = tradeOrderMapper.selectByPickupCode(storeId, today.minusDays(1L), code);
        }
        return order;
    }

    /**
     * 取餐码前置状态判定：只有 READY 可直接核销，其余状态给出可读原因。
     */
    private ScanFailReason notReadyReason(OrderStatus status) {
        if (status == OrderStatus.READY) {
            return null;
        }
        if (status == OrderStatus.COMPLETED) {
            return ScanFailReason.ORDER_COMPLETED;
        }
        if (status == OrderStatus.ACCEPTED || status == OrderStatus.PAID || status == OrderStatus.CREATED) {
            return ScanFailReason.ORDER_NOT_READY;
        }
        return ScanFailReason.ORDER_STATUS_INVALID;
    }

    private ScanResolveVO success(ScanCodeType codeType, String title, String message) {
        ScanResolveVO vo = base(codeType);
        vo.setSuccess(Boolean.TRUE);
        vo.setResultTitle(title);
        vo.setResultMessage(message);
        return vo;
    }

    private ScanResolveVO fail(ScanCodeType codeType, ScanFailReason reason, String message) {
        ScanResolveVO vo = base(codeType);
        vo.setSuccess(Boolean.FALSE);
        vo.setResultTitle("核销失败");
        vo.setResultMessage(StringUtils.hasText(message) ? message : reason.getDefaultText());
        vo.setFailCode(reason.getCode());
        vo.setFailReason(reason.name());
        vo.setFailReasonText(vo.getResultMessage());
        return vo;
    }

    private ScanResolveVO base(ScanCodeType codeType) {
        ScanResolveVO vo = new ScanResolveVO();
        vo.setCodeType(codeType.name());
        vo.setCodeTypeText(codeType.getDescription());
        return vo;
    }

    /**
     * 规范化扫码内容：去空格、转大写。码型判定与下游校验共用同一份规范化结果。
     */
    private static String normalize(String rawCode) {
        if (rawCode == null) {
            return "";
        }
        return rawCode.replace(BLANK_CHAR, "").trim().toUpperCase(Locale.ROOT);
    }

    private static String format(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
