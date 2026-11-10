package com.comeoften.module.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.config.MemberProperties;
import com.comeoften.module.coupon.constant.CouponRuleType;
import com.comeoften.module.coupon.constant.CouponTemplateStatus;
import com.comeoften.module.coupon.dto.CouponGrantRuleSaveDTO;
import com.comeoften.module.coupon.entity.CouponGrantRule;
import com.comeoften.module.coupon.entity.CouponTemplate;
import com.comeoften.module.coupon.mapper.CouponGrantRuleMapper;
import com.comeoften.module.coupon.mapper.CouponTemplateMapper;
import com.comeoften.module.coupon.mapper.UserCouponMapper;
import com.comeoften.module.coupon.service.CouponGrantService;
import com.comeoften.module.coupon.service.CouponRuleService;
import com.comeoften.module.coupon.vo.CouponGrantRuleVO;
import com.comeoften.module.member.constant.MemberTagCode;
import com.comeoften.module.member.service.MemberTagService;
import com.comeoften.module.order.mapper.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 券发放规则实现。
 *
 * <p>幂等键口径（沿用 M6 的 {@code #用途后缀}）：{@code {memberId}#RULE#BIRTHDAY#{yyyy}}、
 * {@code #RULE#SLEEPING#{yyyyMM}}、{@code #RULE#NEW_MEMBER}、{@code #RULE#REPEAT#{orderNo}}。
 * 生日按自然年、沉睡唤醒按自然月各一次，复购按订单一次但受 {@code max_per_member} 限制，
 * 因此「规则改阈值重跑」不会给同一个人重复发券。</p>
 *
 * <p>模板不是「发放中」时整条规则跳过并打 warn，不让一张已停用的券把每日任务打成失败。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRuleServiceImpl implements CouponRuleService {

    private static final int RULE_BATCH_SIZE = 500;

    private static final int DEFAULT_MAX_PER_MEMBER = 1;

    private final CouponGrantRuleMapper couponGrantRuleMapper;

    private final CouponTemplateMapper couponTemplateMapper;

    private final UserCouponMapper userCouponMapper;

    private final CouponGrantService couponGrantService;

    private final MemberTagService memberTagService;

    private final MemberProperties memberProperties;

    private final TradeOrderMapper tradeOrderMapper;

    @Override
    public List<CouponGrantRuleVO> list(Long storeId) {
        List<CouponGrantRule> rules = couponGrantRuleMapper.selectList(
                new LambdaQueryWrapper<CouponGrantRule>()
                        .eq(CouponGrantRule::getStoreId, storeId)
                        .orderByAsc(CouponGrantRule::getId));
        Map<Long, String> names = templateNames(rules);
        List<CouponGrantRuleVO> result = new ArrayList<>();
        for (CouponGrantRule rule : rules) {
            result.add(toVO(rule, names.get(rule.getCouponTemplateId())));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponGrantRuleVO save(Long storeId, CouponGrantRuleSaveDTO dto) {
        CouponRuleType type = CouponRuleType.of(dto.getRuleType());
        if (type == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "触发类型不合法：" + dto.getRuleType());
        }
        CouponTemplate template = couponTemplateMapper.selectById(dto.getCouponTemplateId());
        if (template == null || !storeId.equals(template.getStoreId())) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND);
        }
        int triggerValue = resolveTriggerValue(type, dto.getTriggerValue());
        int maxPerMember = dto.getMaxPerMember() == null
                ? DEFAULT_MAX_PER_MEMBER : dto.getMaxPerMember();
        CouponGrantRule existing = couponGrantRuleMapper.selectByCode(storeId, dto.getRuleCode());
        CouponGrantRule rule = existing == null ? new CouponGrantRule() : existing;
        rule.setStoreId(storeId);
        rule.setRuleCode(dto.getRuleCode());
        rule.setRuleName(dto.getRuleName());
        rule.setRuleType(type.name());
        rule.setCouponTemplateId(dto.getCouponTemplateId());
        rule.setTriggerValue(triggerValue);
        rule.setMaxPerMember(maxPerMember);
        rule.setIsEnabled(Boolean.TRUE.equals(dto.getEnabled()) ? 1
                : (existing == null ? 0 : existing.getIsEnabled()));
        rule.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        rule.setIsDeleted(0);
        try {
            if (existing == null) {
                couponGrantRuleMapper.insert(rule);
            } else {
                couponGrantRuleMapper.updateById(rule);
            }
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.PARAM_INVALID, "规则编码已存在，请换一个", e);
        }
        log.info("券发放规则已保存: storeId={}, ruleCode={}, ruleType={}, templateId={}, enabled={}",
                storeId, rule.getRuleCode(), rule.getRuleType(), rule.getCouponTemplateId(),
                rule.getIsEnabled());
        return toVO(rule, template.getTemplateName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponGrantRuleVO setEnabled(Long storeId, Long ruleId, Boolean enabled) {
        CouponGrantRule rule = couponGrantRuleMapper.selectById(ruleId);
        if (rule == null || !storeId.equals(rule.getStoreId())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "规则不存在");
        }
        rule.setIsEnabled(Boolean.TRUE.equals(enabled) ? 1 : 0);
        couponGrantRuleMapper.updateById(rule);
        return toVO(rule, templateName(rule.getCouponTemplateId()));
    }

    @Override
    public int run(Long storeId, String ruleType) {
        CouponRuleType type = CouponRuleType.of(ruleType);
        if (type == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "触发类型不合法：" + ruleType);
        }
        List<CouponGrantRule> rules = enabledRules(storeId, type);
        if (rules.isEmpty()) {
            return 0;
        }
        int granted = 0;
        for (CouponGrantRule rule : rules) {
            if (!isIssuing(rule)) {
                continue;
            }
            for (Long memberId : targetMembers(type, rule)) {
                granted += grantOnce(rule, memberId, bizId(type, memberId, null),
                        "RULE:" + type.name());
            }
        }
        log.info("券发放规则执行完成: storeId={}, ruleType={}, rules={}, granted={}",
                storeId, type, rules.size(), granted);
        return granted;
    }

    @Override
    public void onFirstPaidOrder(Long memberId, Long storeId, Long orderId) {
        List<CouponGrantRule> rules = enabledRules(storeId, CouponRuleType.NEW_MEMBER);
        for (CouponGrantRule rule : rules) {
            if (!isIssuing(rule)) {
                continue;
            }
            Long paidOrders = tradeOrderMapper.countPaidByMember(memberId);
            if (paidOrders == null || paidOrders > 1L) {
                continue;
            }
            grantOnce(rule, memberId, bizId(CouponRuleType.NEW_MEMBER, memberId, null),
                    "RULE:NEW_MEMBER:" + orderId);
        }
    }

    @Override
    public void onOrderCompleted(Long memberId, Long storeId, String orderNo) {
        List<CouponGrantRule> rules = enabledRules(storeId, CouponRuleType.REPEAT_PURCHASE);
        if (rules.isEmpty()) {
            return;
        }
        Long paidOrders = tradeOrderMapper.countPaidByMember(memberId);
        for (CouponGrantRule rule : rules) {
            if (!isIssuing(rule)) {
                continue;
            }
            int threshold = rule.getTriggerValue() == null ? 2 : rule.getTriggerValue();
            if (paidOrders == null || paidOrders < threshold) {
                continue;
            }
            grantOnce(rule, memberId, bizId(CouponRuleType.REPEAT_PURCHASE, memberId, orderNo),
                    "RULE:REPEAT:" + orderNo);
        }
    }

    // ------------------------------------------------------------------ 私有逻辑

    private List<CouponGrantRule> enabledRules(Long storeId, CouponRuleType type) {
        return couponGrantRuleMapper.selectList(new LambdaQueryWrapper<CouponGrantRule>()
                .eq(CouponGrantRule::getStoreId, storeId)
                .eq(CouponGrantRule::getRuleType, type.name())
                .eq(CouponGrantRule::getIsEnabled, 1));
    }

    private List<Long> targetMembers(CouponRuleType type, CouponGrantRule rule) {
        if (type == CouponRuleType.BIRTHDAY) {
            LocalDate today = LocalDate.now();
            return couponGrantRuleMapper.selectBirthdayMemberIds(today.getMonthValue(),
                    today.getDayOfMonth(), RULE_BATCH_SIZE);
        }
        if (type == CouponRuleType.SLEEPING) {
            return memberTagService.memberIdsByTag(MemberTagCode.SLEEPING, RULE_BATCH_SIZE);
        }
        // 新客与复购由事件驱动，定时任务不产生目标名单
        return Collections.emptyList();
    }

    /**
     * 单会员单规则发放：先按 max_per_member 判次数，再用 grant_biz_id 做最终幂等。
     */
    private int grantOnce(CouponGrantRule rule, Long memberId, String grantBizId, String source) {
        int max = rule.getMaxPerMember() == null ? DEFAULT_MAX_PER_MEMBER : rule.getMaxPerMember();
        Long issued = userCouponMapper.countByMemberAndTemplate(memberId, rule.getCouponTemplateId());
        if (issued != null && issued >= max) {
            return 0;
        }
        return couponGrantService.grantWithBizId(rule.getStoreId(), rule.getCouponTemplateId(),
                memberId, source, grantBizId);
    }

    private boolean isIssuing(CouponGrantRule rule) {
        CouponTemplate template = couponTemplateMapper.selectById(rule.getCouponTemplateId());
        if (template == null
                || !Integer.valueOf(CouponTemplateStatus.ISSUING.getCode()).equals(template.getStatus())) {
            log.warn("规则关联的券模板不是发放中，跳过本条规则: ruleCode={}, templateId={}",
                    rule.getRuleCode(), rule.getCouponTemplateId());
            return false;
        }
        return true;
    }

    private static String bizId(CouponRuleType type, Long memberId, String orderNo) {
        String prefix = memberId + "#RULE#" + type.name();
        switch (type) {
            case BIRTHDAY:
                return prefix + "#" + LocalDate.now().getYear();
            case SLEEPING:
                return prefix + "#" + LocalDateTime.now().format(java.time.format.DateTimeFormatter
                        .ofPattern("yyyyMM"));
            case REPEAT_PURCHASE:
                // 同一订单只发一次的语义；同一会员的多次达标由 max_per_member 收敛
                return prefix + "#" + (orderNo == null ? "" : orderNo);
            default:
                return prefix;
        }
    }

    private int resolveTriggerValue(CouponRuleType type, Integer triggerValue) {
        if (type == CouponRuleType.REPEAT_PURCHASE) {
            int value = triggerValue == null ? 2 : triggerValue;
            if (value < 2) {
                throw new BizException(ErrorCode.PARAM_INVALID, "复购券的订单数阈值至少为 2");
            }
            return value;
        }
        if (type == CouponRuleType.SLEEPING) {
            int value = triggerValue == null || triggerValue <= 0
                    ? memberProperties.getSleepingDays() : triggerValue;
            return value;
        }
        return triggerValue == null ? 0 : Math.max(0, triggerValue);
    }

    private Map<Long, String> templateNames(List<CouponGrantRule> rules) {
        List<Long> ids = rules.stream().map(CouponGrantRule::getCouponTemplateId)
                .filter(id -> id != null).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return couponTemplateMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(CouponTemplate::getId, CouponTemplate::getTemplateName,
                        (left, right) -> left));
    }

    private String templateName(Long templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        return template == null ? "" : template.getTemplateName();
    }

    private CouponGrantRuleVO toVO(CouponGrantRule rule, String templateName) {
        CouponGrantRuleVO vo = new CouponGrantRuleVO();
        vo.setId(rule.getId());
        vo.setRuleCode(rule.getRuleCode());
        vo.setRuleName(rule.getRuleName());
        vo.setRuleType(rule.getRuleType());
        CouponRuleType type = CouponRuleType.of(rule.getRuleType());
        vo.setRuleTypeText(type == null ? "" : type.getDescription());
        vo.setCouponTemplateId(rule.getCouponTemplateId());
        vo.setTemplateName(templateName == null ? "" : templateName);
        vo.setTriggerValue(rule.getTriggerValue());
        vo.setMaxPerMember(rule.getMaxPerMember());
        vo.setIsEnabled(rule.getIsEnabled());
        vo.setPeriodText(periodText(type, rule));
        vo.setRemark(rule.getRemark());
        vo.setCreateTime(rule.getCreateTime());
        return vo;
    }

    private static String periodText(CouponRuleType type, CouponGrantRule rule) {
        if (type == null) {
            return "";
        }
        int max = rule.getMaxPerMember() == null ? DEFAULT_MAX_PER_MEMBER : rule.getMaxPerMember();
        switch (type) {
            case BIRTHDAY:
                return "每年生日当天发放，每会员最多 " + max + " 张";
            case SLEEPING:
                return "每自然月对沉睡会员发放，每会员最多 " + max + " 张";
            case REPEAT_PURCHASE:
                return "累计支付满 " + (rule.getTriggerValue() == null ? 2 : rule.getTriggerValue())
                        + " 单后发放，每会员最多 " + max + " 张";
            default:
                return "首单支付成功后发放，每会员最多 " + max + " 张";
        }
    }
}
