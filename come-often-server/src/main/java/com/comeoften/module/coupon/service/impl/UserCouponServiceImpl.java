package com.comeoften.module.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.PageResult;
import com.comeoften.common.util.QrCodeUtils;
import com.comeoften.config.CouponProperties;
import com.comeoften.module.coupon.constant.CouponConstants;
import com.comeoften.module.coupon.constant.CouponTemplateStatus;
import com.comeoften.module.coupon.constant.CouponType;
import com.comeoften.module.coupon.constant.UserCouponStatus;
import com.comeoften.module.coupon.converter.CouponConverter;
import com.comeoften.module.coupon.entity.CouponTemplate;
import com.comeoften.module.coupon.entity.CouponTemplateDish;
import com.comeoften.module.coupon.entity.UserCoupon;
import com.comeoften.module.coupon.mapper.CouponTemplateDishMapper;
import com.comeoften.module.coupon.mapper.CouponTemplateMapper;
import com.comeoften.module.coupon.mapper.UserCouponMapper;
import com.comeoften.module.coupon.service.CouponGrantService;
import com.comeoften.module.coupon.service.UserCouponService;
import com.comeoften.module.coupon.util.CouponCodeSigner;
import com.comeoften.module.coupon.util.CouponDiscountCalculator;
import com.comeoften.module.coupon.vo.CouponCenterItemVO;
import com.comeoften.module.coupon.vo.CouponCodeVO;
import com.comeoften.module.coupon.vo.CouponSelectionVO;
import com.comeoften.module.coupon.vo.UserCouponVO;
import com.comeoften.module.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 会员持券实现。状态写一律用带旧状态条件的 UPDATE；券码签名在服务端按时间窗现算，不落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl implements UserCouponService {

    private static final String TAB_UNUSED = "unused";

    private static final String TAB_USED = "used";

    private static final String TAB_EXPIRED = "expired";

    private static final long PAGE_MAX_SIZE = 50L;

    private final UserCouponMapper userCouponMapper;

    private final CouponTemplateMapper couponTemplateMapper;

    private final CouponTemplateDishMapper couponTemplateDishMapper;

    private final CouponGrantService couponGrantService;

    private final CouponProperties couponProperties;

    private final StoreService storeService;

    /**
     * 券码二维码边长（像素）。240 在 750 宽的卡片里放大后仍清晰，base64 体积约 1KB。
     */
    private static final int QR_IMAGE_SIZE = 240;

    @Override
    public List<CouponCenterItemVO> listCenter(Long memberId, Long storeId) {
        LocalDateTime now = LocalDateTime.now();
        List<CouponTemplate> templates = couponTemplateMapper.selectList(
                new LambdaQueryWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getStoreId, storeId)
                        .eq(CouponTemplate::getStatus, CouponTemplateStatus.ISSUING.getCode())
                        .eq(CouponTemplate::getIsDeleted, 0)
                        .and(wrapper -> wrapper.isNull(CouponTemplate::getStartTime)
                                .or().le(CouponTemplate::getStartTime, now))
                        .and(wrapper -> wrapper.isNull(CouponTemplate::getEndTime)
                                .or().ge(CouponTemplate::getEndTime, now))
                        .gt(CouponTemplate::getRemain, 0)
                        .orderByDesc(CouponTemplate::getCreateTime));
        if (templates.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> templateIds = templates.stream().map(CouponTemplate::getId)
                .collect(Collectors.toList());
        Set<Long> receivedIds = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getMemberId, memberId)
                .eq(UserCoupon::getStoreId, storeId)
                .eq(UserCoupon::getIsDeleted, 0)
                .in(UserCoupon::getTemplateId, templateIds))
                .stream().map(UserCoupon::getTemplateId).collect(Collectors.toSet());
        return templates.stream()
                .map(template -> CouponConverter.toCenterItemVO(template,
                        receivedIds.contains(template.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCouponVO receive(Long memberId, Long storeId, Long templateId) {
        storeService.requireOpen(storeId);
        CouponTemplate template = requireReceivable(storeId, templateId);
        int granted = couponGrantService.grant(storeId, templateId,
                Collections.singletonList(memberId), "APP_RECEIVE");
        if (granted <= 0) {
            throw new BizException(ErrorCode.COUPON_SOLD_OUT);
        }
        UserCoupon latest = userCouponMapper.selectOne(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getMemberId, memberId)
                .eq(UserCoupon::getTemplateId, templateId)
                .orderByDesc(UserCoupon::getCreateTime)
                .last("LIMIT 1"));
        if (latest == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "领券结果查询失败，请稍后在卡包查看");
        }
        log.info("会员领券成功: memberId={}, templateId={}, userCouponId={}",
                memberId, templateId, latest.getId());
        return CouponConverter.toUserCouponVO(latest, template);
    }

    @Override
    public PageResult<UserCouponVO> pageForMember(Long memberId, Long storeId, String tab,
                                                  Long current, Long size) {
        Page<UserCoupon> page = new Page<>(Math.max(1L, current == null ? 1L : current), boundSize(size));
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getMemberId, memberId)
                .eq(storeId != null, UserCoupon::getStoreId, storeId);
        if (TAB_UNUSED.equals(tab)) {
            wrapper.eq(UserCoupon::getStatus, UserCouponStatus.AVAILABLE.getCode())
                    .gt(UserCoupon::getExpireAt, now);
        } else if (TAB_USED.equals(tab)) {
            wrapper.eq(UserCoupon::getStatus, UserCouponStatus.USED.getCode());
        } else if (TAB_EXPIRED.equals(tab)) {
            wrapper.and(inner -> inner.eq(UserCoupon::getStatus, UserCouponStatus.EXPIRED.getCode())
                    .or(pending -> pending.eq(UserCoupon::getStatus, UserCouponStatus.AVAILABLE.getCode())
                            .le(UserCoupon::getExpireAt, now)));
        }
        wrapper.orderByDesc(UserCoupon::getCreateTime);
        IPage<UserCoupon> result = userCouponMapper.selectPage(page, wrapper);
        Map<Long, CouponTemplate> templates = loadTemplates(result.getRecords().stream()
                .map(UserCoupon::getTemplateId).distinct().collect(Collectors.toList()));
        List<UserCouponVO> records = new ArrayList<>();
        for (UserCoupon entity : result.getRecords()) {
            records.add(CouponConverter.toUserCouponVO(entity, templates.get(entity.getTemplateId())));
        }
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponCodeVO currentCode(Long memberId, Long userCouponId) {
        UserCoupon userCoupon = requireOwned(memberId, userCouponId);
        if (Integer.valueOf(UserCouponStatus.USED.getCode()).equals(userCoupon.getStatus())) {
            throw new BizException(ErrorCode.COUPON_ALREADY_USED);
        }
        LocalDateTime now = LocalDateTime.now();
        if (userCoupon.getExpireAt() != null && !userCoupon.getExpireAt().isAfter(now)) {
            userCouponMapper.markExpired(userCoupon.getId());
            throw new BizException(ErrorCode.COUPON_EXPIRED);
        }
        CouponTemplate template = couponTemplateMapper.selectById(userCoupon.getTemplateId());
        long epochSecond = System.currentTimeMillis() / 1000L;
        String code = CouponCodeSigner.buildCode(userCoupon.getCodeSeq(), epochSecond, requireSecret());
        CouponCodeVO vo = new CouponCodeVO();
        vo.setUserCouponId(userCoupon.getId());
        vo.setTemplateId(userCoupon.getTemplateId());
        vo.setCode(code);
        vo.setCodeContent(CouponConstants.CODE_PREFIX + code);
        vo.setQrImageBase64(qrImageBase64(vo.getCodeContent()));
        vo.setSecondsRemaining(CouponCodeSigner.secondsRemaining(epochSecond));
        vo.setExpireAt(userCoupon.getExpireAt());
        if (template != null) {
            vo.setTemplateName(template.getTemplateName());
            vo.setCouponTypeText(CouponType.describe(template.getCouponType()));
            vo.setFaceText(CouponDiscountCalculator.faceText(template));
            vo.setConditionText(CouponDiscountCalculator.conditionText(template));
            vo.setFaceValue(template.getFaceValue());
        }
        return vo;
    }

    /**
     * 二维码只是展示增强，出图失败不能让整张券不可用：降级为只显示数字券码，人工输码仍可核销。
     */
    private static String qrImageBase64(String content) {
        try {
            return QrCodeUtils.pngBase64(content, QR_IMAGE_SIZE);
        } catch (RuntimeException exception) {
            log.warn("券码二维码生成失败，降级为数字券码: content={}", content, exception);
            return null;
        }
    }

    @Override
    public List<UserCouponVO> listUsable(Long memberId, Long storeId, BigDecimal orderAmount,
                                         Map<Long, BigDecimal> dishAmounts) {
        if (orderAmount == null || orderAmount.signum() <= 0) {
            return Collections.emptyList();
        }
        List<UserCoupon> coupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getMemberId, memberId)
                .eq(storeId != null, UserCoupon::getStoreId, storeId)
                .eq(UserCoupon::getStatus, UserCouponStatus.AVAILABLE.getCode())
                .gt(UserCoupon::getExpireAt, LocalDateTime.now())
                .orderByAsc(UserCoupon::getExpireAt));
        if (coupons.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, CouponTemplate> templates = loadTemplates(coupons.stream()
                .map(UserCoupon::getTemplateId).distinct().collect(Collectors.toList()));
        Map<Long, List<Long>> templateDishes = loadTemplateDishes(new ArrayList<>(templates.keySet()));
        List<UserCouponVO> usable = new ArrayList<>();
        List<UserCouponVO> unusable = new ArrayList<>();
        for (UserCoupon coupon : coupons) {
            CouponTemplate template = templates.get(coupon.getTemplateId());
            UserCouponVO vo = CouponConverter.toUserCouponVO(coupon, template);
            if (template == null) {
                vo.setUsable(false);
                vo.setUnusableReason("券模板已删除");
                unusable.add(vo);
                continue;
            }
            try {
                BigDecimal discount = CouponDiscountCalculator.discount(template, orderAmount,
                        dishAmount(template, templateDishes.get(template.getId()), dishAmounts));
                vo.setUsable(true);
                vo.setDiscountAmount(discount);
                usable.add(vo);
            } catch (BizException e) {
                vo.setUsable(false);
                vo.setUnusableReason(e.getMessage());
                unusable.add(vo);
            }
        }
        usable.sort((left, right) -> right.getDiscountAmount().compareTo(left.getDiscountAmount()));
        List<UserCouponVO> result = new ArrayList<>(usable);
        result.addAll(unusable);
        return result;
    }

    @Override
    public CouponSelectionVO resolveForOrder(Long memberId, Long storeId, Long userCouponId,
                                             BigDecimal orderAmount, Map<Long, BigDecimal> dishAmounts) {
        UserCoupon userCoupon = requireOwned(memberId, userCouponId);
        if (!storeId.equals(userCoupon.getStoreId())) {
            throw new BizException(ErrorCode.COUPON_STORE_MISMATCH);
        }
        if (Integer.valueOf(UserCouponStatus.USED.getCode()).equals(userCoupon.getStatus())) {
            throw new BizException(ErrorCode.COUPON_ALREADY_USED);
        }
        boolean expired = Integer.valueOf(UserCouponStatus.EXPIRED.getCode()).equals(userCoupon.getStatus())
                || (userCoupon.getExpireAt() != null && !userCoupon.getExpireAt().isAfter(LocalDateTime.now()));
        if (expired) {
            throw new BizException(ErrorCode.COUPON_EXPIRED);
        }
        CouponTemplate template = couponTemplateMapper.selectById(userCoupon.getTemplateId());
        if (template == null) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND);
        }
        if (Integer.valueOf(CouponTemplateStatus.DRAFT.getCode()).equals(template.getStatus())) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_NOT_ISSUING, "券模板尚未发布，无法使用");
        }
        List<Long> relationDishIds = couponTemplateDishMapper.selectList(
                        new LambdaQueryWrapper<CouponTemplateDish>()
                                .eq(CouponTemplateDish::getTemplateId, template.getId()))
                .stream().map(CouponTemplateDish::getDishId).collect(Collectors.toList());
        BigDecimal discount = CouponDiscountCalculator.discount(template, orderAmount,
                dishAmount(template, relationDishIds, dishAmounts));
        return CouponConverter.toSelectionVO(userCoupon, template, discount);
    }

    @Override
    public void lockForOrder(Long memberId, Long userCouponId, Long orderId) {
        int affected = userCouponMapper.lockForOrder(userCouponId, memberId, orderId, LocalDateTime.now());
        if (affected == 0) {
            throw new BizException(ErrorCode.COUPON_ALREADY_USED, "券已被使用或不属于当前会员");
        }
        log.info("下单锁定券: memberId={}, userCouponId={}, orderId={}", memberId, userCouponId, orderId);
    }

    @Override
    public void releaseForOrder(Long userCouponId, Long orderId) {
        if (userCouponId == null || orderId == null) {
            return;
        }
        int affected = userCouponMapper.releaseForOrder(userCouponId, orderId);
        if (affected > 0) {
            log.info("订单取消退回券: userCouponId={}, orderId={}", userCouponId, orderId);
        }
    }

    private CouponTemplate requireReceivable(Long storeId, Long templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null || !storeId.equals(template.getStoreId())) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND);
        }
        if (!Integer.valueOf(CouponTemplateStatus.ISSUING.getCode()).equals(template.getStatus())) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_NOT_ISSUING);
        }
        LocalDateTime now = LocalDateTime.now();
        if (template.getStartTime() != null && now.isBefore(template.getStartTime())) {
            throw new BizException(ErrorCode.COUPON_RECEIVE_CLOSED, "券尚未开始发放");
        }
        if (template.getEndTime() != null && now.isAfter(template.getEndTime())) {
            throw new BizException(ErrorCode.COUPON_RECEIVE_CLOSED, "券已结束发放");
        }
        return template;
    }

    private UserCoupon requireOwned(Long memberId, Long userCouponId) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) {
            throw new BizException(ErrorCode.COUPON_NOT_FOUND);
        }
        if (!memberId.equals(userCoupon.getMemberId())) {
            throw new BizException(ErrorCode.COUPON_NOT_FOUND, "券不属于当前会员");
        }
        return userCoupon;
    }

    private Map<Long, CouponTemplate> loadTemplates(List<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return new HashMap<>(0);
        }
        return couponTemplateMapper.selectBatchIds(templateIds).stream()
                .collect(Collectors.toMap(CouponTemplate::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, List<Long>> loadTemplateDishes(List<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return new HashMap<>(0);
        }
        Map<Long, List<Long>> result = new HashMap<>(templateIds.size());
        for (CouponTemplateDish relation : couponTemplateDishMapper.selectList(
                new LambdaQueryWrapper<CouponTemplateDish>()
                        .in(CouponTemplateDish::getTemplateId, templateIds))) {
            result.computeIfAbsent(relation.getTemplateId(), key -> new ArrayList<>())
                    .add(relation.getDishId());
        }
        return result;
    }

    /**
     * 单品券的抵扣基数：dish_id 与 coupon_template_dish 指定菜品的行金额小计。
     */
    private BigDecimal dishAmount(CouponTemplate template, List<Long> relationDishIds,
                                  Map<Long, BigDecimal> dishAmounts) {
        if (dishAmounts == null || dishAmounts.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<Long> dishIds = new ArrayList<>();
        if (template.getDishId() != null) {
            dishIds.add(template.getDishId());
        }
        if (relationDishIds != null) {
            dishIds.addAll(relationDishIds);
        }
        return dishIds.stream().distinct()
                .map(dishAmounts::get)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String requireSecret() {
        if (!StringUtils.hasText(couponProperties.getCodeSecret())) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "券码密钥未配置");
        }
        return couponProperties.getCodeSecret();
    }

    private static long boundSize(Long size) {
        long value = size == null ? 10L : size;
        return Math.min(Math.max(1L, value), PAGE_MAX_SIZE);
    }
}
