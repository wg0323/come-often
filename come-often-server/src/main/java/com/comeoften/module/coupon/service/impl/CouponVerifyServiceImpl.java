package com.comeoften.module.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.config.CouponProperties;
import com.comeoften.module.coupon.constant.CouponConstants;
import com.comeoften.module.coupon.constant.UserCouponStatus;
import com.comeoften.module.coupon.constant.VerifyChannel;
import com.comeoften.module.coupon.converter.CouponConverter;
import com.comeoften.module.coupon.dto.CouponVerifyDTO;
import com.comeoften.module.coupon.entity.CouponTemplate;
import com.comeoften.module.coupon.entity.CouponTemplateDish;
import com.comeoften.module.coupon.entity.CouponUseRecord;
import com.comeoften.module.coupon.entity.UserCoupon;
import com.comeoften.module.coupon.mapper.CouponTemplateDishMapper;
import com.comeoften.module.coupon.mapper.CouponTemplateMapper;
import com.comeoften.module.coupon.mapper.CouponUseRecordMapper;
import com.comeoften.module.coupon.mapper.UserCouponMapper;
import com.comeoften.module.coupon.service.CouponVerifyService;
import com.comeoften.module.coupon.util.CouponCodeSigner;
import com.comeoften.module.coupon.util.CouponDiscountCalculator;
import com.comeoften.module.coupon.vo.CouponVerifyVO;
import com.comeoften.module.order.entity.OrderItem;
import com.comeoften.module.order.entity.TradeOrder;
import com.comeoften.module.order.mapper.OrderItemMapper;
import com.comeoften.module.order.mapper.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 券核销实现。校验顺序固定：存在 → 归属门店 → 状态 → 券码时间窗 → 门槛 → 条件更新。
 * 状态判定放在签名之前，已核销的券再次扫码/输码统一返回「已使用」，不会因为码过期而报验签失败。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponVerifyServiceImpl implements CouponVerifyService {

    private final UserCouponMapper userCouponMapper;

    private final CouponTemplateMapper couponTemplateMapper;

    private final CouponTemplateDishMapper couponTemplateDishMapper;

    private final CouponUseRecordMapper couponUseRecordMapper;

    private final TradeOrderMapper tradeOrderMapper;

    private final OrderItemMapper orderItemMapper;

    private final CouponProperties couponProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponVerifyVO verify(Long storeId, Long operatorUserId, CouponVerifyDTO dto,
                                 VerifyChannel channel) {
        String code = normalize(dto.getCode());
        String codeSeq = code.substring(0, CouponConstants.CODE_SEQ_LENGTH);
        String sign = code.substring(CouponConstants.CODE_SEQ_LENGTH);
        UserCoupon userCoupon = userCouponMapper.selectOne(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getCodeSeq, codeSeq));
        if (userCoupon == null) {
            throw new BizException(ErrorCode.COUPON_NOT_FOUND, "券码不存在");
        }
        if (!storeId.equals(userCoupon.getStoreId())) {
            throw new BizException(ErrorCode.COUPON_STORE_MISMATCH);
        }
        if (Integer.valueOf(UserCouponStatus.USED.getCode()).equals(userCoupon.getStatus())) {
            throw new BizException(ErrorCode.COUPON_ALREADY_USED);
        }
        LocalDateTime now = LocalDateTime.now();
        if (Integer.valueOf(UserCouponStatus.EXPIRED.getCode()).equals(userCoupon.getStatus())
                || (userCoupon.getExpireAt() != null && !userCoupon.getExpireAt().isAfter(now))) {
            userCouponMapper.markExpired(userCoupon.getId());
            throw new BizException(ErrorCode.COUPON_EXPIRED);
        }
        if (!CouponCodeSigner.matches(codeSeq, sign, System.currentTimeMillis() / 1000L,
                requireSecret())) {
            throw new BizException(ErrorCode.COUPON_CODE_INVALID);
        }
        CouponTemplate template = couponTemplateMapper.selectById(userCoupon.getTemplateId());
        if (template == null) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND);
        }
        TradeOrder order = loadOrder(storeId, dto.getOrderNo());
        BigDecimal discountAmount = resolveDiscount(template, order);
        // 条件更新是并发幂等的唯一判据：并发核销只有一个能成功
        if (userCouponMapper.markUsedForVerify(userCoupon.getId(), now) == 0) {
            throw new BizException(ErrorCode.COUPON_ALREADY_USED);
        }
        CouponUseRecord record = new CouponUseRecord();
        record.setUserCouponId(userCoupon.getId());
        record.setMemberId(userCoupon.getMemberId());
        record.setStoreId(storeId);
        record.setOrderId(order == null ? null : order.getId());
        record.setDiscountAmount(discountAmount);
        record.setOperatorUserId(operatorUserId);
        record.setVerifyChannel(channel.name());
        record.setVerifyTime(now);
        try {
            couponUseRecordMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // uk_user_coupon 兜住并发：另一笔核销已经写入记录，整体回滚
            throw new BizException(ErrorCode.COUPON_ALREADY_USED, "券已被核销", e);
        }
        userCoupon.setStatus(UserCouponStatus.USED.getCode());
        userCoupon.setUseTime(now);
        log.info("券核销成功: userCouponId={}, templateId={}, storeId={}, operatorUserId={}, "
                        + "channel={}, discountAmount={}", userCoupon.getId(), template.getId(), storeId,
                operatorUserId, channel, discountAmount);
        CouponVerifyVO vo = CouponConverter.toVerifyVO(userCoupon, template, discountAmount, channel);
        vo.setOperatorUserId(operatorUserId);
        return vo;
    }

    /**
     * 带订单核销时按订单金额算真实抵扣，不带订单时按券面额记录。
     */
    private BigDecimal resolveDiscount(CouponTemplate template, TradeOrder order) {
        if (order == null) {
            return CouponDiscountCalculator.verifyAmount(template);
        }
        return CouponDiscountCalculator.discount(template, order.getTotalAmount(),
                dishAmount(template, order.getId()));
    }

    private BigDecimal dishAmount(CouponTemplate template, Long orderId) {
        List<Long> dishIds = new ArrayList<>();
        if (template.getDishId() != null) {
            dishIds.add(template.getDishId());
        }
        dishIds.addAll(couponTemplateDishMapper.selectList(new LambdaQueryWrapper<CouponTemplateDish>()
                        .eq(CouponTemplateDish::getTemplateId, template.getId()))
                .stream().map(CouponTemplateDish::getDishId).collect(Collectors.toList()));
        if (dishIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId)
                        .in(OrderItem::getDishId, dishIds))
                .stream().map(OrderItem::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private TradeOrder loadOrder(Long storeId, String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return null;
        }
        TradeOrder order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getOrderNo, orderNo));
        if (order == null || !storeId.equals(order.getStoreId())) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /**
     * 规范化券码：去空格、转大写、去掉商家端扫码用的 C 前缀。
     */
    private static String normalize(String raw) {
        String value = raw == null ? "" : raw.replace(" ", "").toUpperCase(Locale.ROOT);
        int expectLength = CouponConstants.CODE_SEQ_LENGTH + CouponConstants.SIGN_LENGTH;
        if (value.startsWith(CouponConstants.CODE_PREFIX) && value.length() == expectLength + 1) {
            value = value.substring(CouponConstants.CODE_PREFIX.length());
        }
        if (value.length() != expectLength) {
            throw new BizException(ErrorCode.COUPON_CODE_INVALID, "券码长度不合法");
        }
        return value;
    }

    private String requireSecret() {
        if (!StringUtils.hasText(couponProperties.getCodeSecret())) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "券码密钥未配置");
        }
        return couponProperties.getCodeSecret();
    }
}
