package com.comeoften.module.coupon.converter;

import com.comeoften.module.coupon.constant.CouponTemplateStatus;
import com.comeoften.module.coupon.constant.CouponType;
import com.comeoften.module.coupon.constant.UserCouponStatus;
import com.comeoften.module.coupon.constant.VerifyChannel;
import com.comeoften.module.coupon.entity.CouponTemplate;
import com.comeoften.module.coupon.entity.CouponUseRecord;
import com.comeoften.module.coupon.entity.UserCoupon;
import com.comeoften.module.coupon.util.CouponDiscountCalculator;
import com.comeoften.module.coupon.vo.CouponCenterItemVO;
import com.comeoften.module.coupon.vo.CouponRoiVO;
import com.comeoften.module.coupon.vo.CouponSelectionVO;
import com.comeoften.module.coupon.vo.CouponTemplateVO;
import com.comeoften.module.coupon.vo.CouponUseRecordVO;
import com.comeoften.module.coupon.vo.CouponVerifyVO;
import com.comeoften.module.coupon.vo.UserCouponVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 券对象转换。文案、文案型状态与 ROI 派生值都在这里算，页面与后台只展示。
 */
public final class CouponConverter {

    private static final int RATE_SCALE = 2;

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final String GROUP_MEAL_KEYWORD = "团餐";

    private CouponConverter() {
    }

    public static CouponTemplateVO toTemplateVO(CouponTemplate template, List<Long> dishIds) {
        CouponTemplateVO vo = new CouponTemplateVO();
        vo.setId(template.getId());
        vo.setTemplateName(template.getTemplateName());
        vo.setCouponType(template.getCouponType());
        vo.setCouponTypeText(CouponType.describe(template.getCouponType()));
        vo.setFaceValue(template.getFaceValue());
        vo.setDiscountRate(template.getDiscountRate());
        vo.setThresholdAmount(template.getThresholdAmount());
        vo.setDishId(template.getDishId());
        vo.setDishIds(dishIds.stream().map(String::valueOf).collect(Collectors.toList()));
        vo.setTotal(template.getTotal());
        vo.setRemain(template.getRemain());
        vo.setValidDays(template.getValidDays());
        vo.setStartTime(template.getStartTime());
        vo.setEndTime(template.getEndTime());
        vo.setStackable(template.getStackable());
        vo.setStatus(template.getStatus());
        vo.setStatusText(CouponTemplateStatus.describe(template.getStatus()));
        vo.setCreateTime(template.getCreateTime());
        return vo;
    }

    public static CouponCenterItemVO toCenterItemVO(CouponTemplate template, boolean received) {
        CouponCenterItemVO vo = new CouponCenterItemVO();
        vo.setTemplateId(template.getId());
        vo.setTemplateName(template.getTemplateName());
        vo.setCouponType(template.getCouponType());
        vo.setCouponTypeText(CouponType.describe(template.getCouponType()));
        vo.setFaceValue(template.getFaceValue());
        vo.setDiscountRate(template.getDiscountRate());
        vo.setThresholdAmount(template.getThresholdAmount());
        vo.setFaceText(CouponDiscountCalculator.faceText(template));
        vo.setConditionText(CouponDiscountCalculator.conditionText(template));
        vo.setStartTime(template.getStartTime());
        vo.setEndTime(template.getEndTime());
        vo.setTotal(template.getTotal());
        vo.setRemain(template.getRemain());
        vo.setValidDays(template.getValidDays());
        vo.setReceived(received);
        vo.setCanReceive(!received && template.getRemain() != null && template.getRemain() > 0);
        vo.setGroupMeal(template.getTemplateName() != null
                && template.getTemplateName().contains(GROUP_MEAL_KEYWORD));
        return vo;
    }

    public static UserCouponVO toUserCouponVO(UserCoupon userCoupon, CouponTemplate template) {
        UserCouponVO vo = new UserCouponVO();
        vo.setUserCouponId(userCoupon.getId());
        vo.setTemplateId(userCoupon.getTemplateId());
        vo.setStatus(userCoupon.getStatus());
        vo.setStatusText(UserCouponStatus.describe(userCoupon.getStatus()));
        vo.setReceiveTime(userCoupon.getReceiveTime());
        vo.setExpireAt(userCoupon.getExpireAt());
        vo.setUseTime(userCoupon.getUseTime());
        vo.setOrderId(userCoupon.getOrderId());
        if (template != null) {
            vo.setTemplateName(template.getTemplateName());
            vo.setCouponType(template.getCouponType());
            vo.setCouponTypeText(CouponType.describe(template.getCouponType()));
            vo.setFaceValue(template.getFaceValue());
            vo.setDiscountRate(template.getDiscountRate());
            vo.setThresholdAmount(template.getThresholdAmount());
            vo.setDishId(template.getDishId());
            vo.setStackable(template.getStackable());
            vo.setFaceText(CouponDiscountCalculator.faceText(template));
            vo.setConditionText(CouponDiscountCalculator.conditionText(template));
        }
        return vo;
    }

    public static CouponUseRecordVO toUseRecordVO(CouponUseRecord record, String templateName,
                                                   String couponType, String operatorName) {
        CouponUseRecordVO vo = new CouponUseRecordVO();
        vo.setId(record.getId());
        vo.setUserCouponId(record.getUserCouponId());
        vo.setTemplateName(templateName == null ? "" : templateName);
        vo.setCouponTypeText(CouponType.describe(couponType));
        vo.setMemberId(record.getMemberId());
        vo.setOrderId(record.getOrderId());
        vo.setDiscountAmount(record.getDiscountAmount());
        vo.setOperatorUserId(record.getOperatorUserId());
        vo.setOperatorName(operatorName == null ? "" : operatorName);
        vo.setVerifyChannel(record.getVerifyChannel());
        vo.setVerifyChannelText(VerifyChannel.describe(record.getVerifyChannel()));
        vo.setVerifyTime(record.getVerifyTime());
        return vo;
    }

    public static CouponVerifyVO toVerifyVO(UserCoupon userCoupon, CouponTemplate template,
                                            BigDecimal discountAmount, VerifyChannel channel) {
        CouponVerifyVO vo = new CouponVerifyVO();
        vo.setUserCouponId(userCoupon.getId());
        vo.setTemplateId(userCoupon.getTemplateId());
        vo.setMemberId(userCoupon.getMemberId());
        vo.setStoreId(userCoupon.getStoreId());
        vo.setVerifyChannel(channel.name());
        vo.setVerifyChannelText(channel.getDescription());
        vo.setVerifyTime(userCoupon.getUseTime());
        vo.setDiscountAmount(discountAmount);
        if (template != null) {
            vo.setTemplateName(template.getTemplateName());
            vo.setCouponType(template.getCouponType());
            vo.setCouponTypeText(CouponType.describe(template.getCouponType()));
            vo.setFaceValue(template.getFaceValue());
            vo.setThresholdAmount(template.getThresholdAmount());
        }
        return vo;
    }

    public static CouponSelectionVO toSelectionVO(UserCoupon userCoupon, CouponTemplate template,
                                                  BigDecimal discountAmount) {
        CouponSelectionVO vo = new CouponSelectionVO();
        vo.setUserCouponId(userCoupon.getId());
        vo.setTemplateId(userCoupon.getTemplateId());
        vo.setDiscountAmount(discountAmount);
        if (template != null) {
            vo.setTemplateName(template.getTemplateName());
            vo.setCouponType(template.getCouponType());
            vo.setCouponTypeText(CouponType.describe(template.getCouponType()));
            vo.setStackable(template.getStackable());
        }
        return vo;
    }

    /**
     * ROI 派生值：投产比 = 实收 / 抵扣，核销率 = 已使用 / 已领取。
     */
    public static CouponRoiVO toRoiVO(CouponRoiVO source) {
        CouponRoiVO vo = new CouponRoiVO();
        vo.setTemplateId(source.getTemplateId());
        vo.setTemplateName(source.getTemplateName());
        vo.setCouponType(source.getCouponType());
        vo.setCouponTypeText(CouponType.describe(source.getCouponType()));
        vo.setTotal(source.getTotal());
        vo.setRemain(source.getRemain());
        vo.setReceivedCount(source.getReceivedCount() == null ? 0L : source.getReceivedCount());
        vo.setUsedCount(source.getUsedCount() == null ? 0L : source.getUsedCount());
        vo.setOrderCount(source.getOrderCount() == null ? 0L : source.getOrderCount());
        vo.setDiscountAmount(nullToZero(source.getDiscountAmount()));
        vo.setRevenueAmount(nullToZero(source.getRevenueAmount()));
        if (vo.getDiscountAmount().signum() > 0) {
            vo.setRoi(vo.getRevenueAmount().divide(vo.getDiscountAmount(), RATE_SCALE, RoundingMode.HALF_UP));
        }
        if (vo.getReceivedCount() > 0L) {
            vo.setUseRate(new BigDecimal(vo.getUsedCount())
                    .multiply(HUNDRED)
                    .divide(new BigDecimal(vo.getReceivedCount()), RATE_SCALE, RoundingMode.HALF_UP));
        } else {
            vo.setUseRate(BigDecimal.ZERO);
        }
        return vo;
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP) : value;
    }
}
