package com.comeoften.module.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.StockKeys;
import com.comeoften.config.CouponProperties;
import com.comeoften.module.coupon.constant.CouponConstants;
import com.comeoften.module.coupon.constant.CouponTemplateStatus;
import com.comeoften.module.coupon.entity.CouponTemplate;
import com.comeoften.module.coupon.entity.UserCoupon;
import com.comeoften.module.coupon.mapper.CouponTemplateMapper;
import com.comeoften.module.coupon.mapper.UserCouponMapper;
import com.comeoften.module.coupon.service.CouponGrantService;
import com.comeoften.module.order.mapper.TradeOrderMapper;
import com.comeoften.mq.MessageProducer;
import com.comeoften.mq.RabbitConst;
import com.comeoften.mq.message.CouponExpireMessage;
import com.comeoften.mq.message.MqEnvelope;
import com.comeoften.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 发券实现。三类库存同一口径：Redis 只作快速判定，coupon_template.remain 是最终事实源；
 * 领取失败或事务回滚必须把预扣的 Redis 值补回去。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponGrantServiceImpl implements CouponGrantService {

    private static final int DEFAULT_VALID_DAYS = 30;

    private static final long CODE_SEQ_BOUND = 1000000L;

    private static final long MAX_REPLAY_DELAY_SECONDS = 3600L;

    private final CouponTemplateMapper couponTemplateMapper;

    private final UserCouponMapper userCouponMapper;

    private final TradeOrderMapper tradeOrderMapper;

    private final StockService stockService;

    private final MessageProducer messageProducer;

    private final CouponProperties couponProperties;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int grant(Long storeId, Long templateId, List<Long> memberIds, String source) {
        CouponTemplate template = requireIssuingTemplate(storeId, templateId);
        return doGrant(template, memberIds, source, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int grantWithBizId(Long storeId, Long templateId, Long memberId, String source,
                              String grantBizId) {
        if (memberId == null || !StringUtils.hasText(grantBizId)) {
            return 0;
        }
        CouponTemplate template = requireIssuingTemplate(storeId, templateId);
        return doGrant(template, Collections.singletonList(memberId), source, grantBizId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int grantNewcomerIfFirstPaidOrder(Long memberId, Long storeId, Long orderId) {
        if (memberId == null || storeId == null) {
            return 0;
        }
        CouponTemplate template = resolveNewcomerTemplate(storeId);
        if (template == null) {
            return 0;
        }
        Long paidOrders = tradeOrderMapper.countPaidByMember(memberId);
        if (paidOrders == null || paidOrders > 1L) {
            log.info("非首单，跳过新人券发放: memberId={}, paidOrders={}", memberId, paidOrders);
            return 0;
        }
        Long received = userCouponMapper.countByMemberAndTemplate(memberId, template.getId());
        if (received != null && received > 0L) {
            log.info("新人券已发过，跳过: memberId={}, templateId={}", memberId, template.getId());
            return 0;
        }
        int granted = doGrant(template, Collections.singletonList(memberId), "NEWCOMER:" + orderId, null);
        log.info("新人券发放完成: memberId={}, templateId={}, granted={}", memberId, template.getId(), granted);
        return granted;
    }

    @Override
    public void resendExpireRemind(Long userCouponId, long delaySeconds) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null) {
            throw new BizException(ErrorCode.COUPON_NOT_FOUND);
        }
        CouponTemplate template = couponTemplateMapper.selectById(userCoupon.getTemplateId());
        long bounded = Math.max(1L, Math.min(delaySeconds, MAX_REPLAY_DELAY_SECONDS));
        publishExpireMessage(userCoupon, template, TimeUnit.SECONDS.toMillis(bounded));
    }

    /**
     * 逐会员发放。库存不足即停止，已发出的部分不回滚，符合「发到发完为止」的业务语义。
     */
    private int doGrant(CouponTemplate template, List<Long> memberIds, String source,
                        String grantBizId) {
        if (memberIds == null || memberIds.isEmpty()) {
            return 0;
        }
        if (grantBizId != null && userCouponMapper.countByGrantBizId(grantBizId) > 0) {
            log.info("规则发券已发过，跳过: templateId={}, grantBizId={}", template.getId(), grantBizId);
            return 0;
        }
        Set<Long> distinct = new LinkedHashSet<>(memberIds);
        List<String> deductedKeys = new ArrayList<>();
        registerRollbackCompensation(deductedKeys);
        int granted = 0;
        for (Long memberId : distinct) {
            if (memberId == null) {
                continue;
            }
            if (!tryDeductStock(template, deductedKeys)) {
                log.info("券库存不足，停止发放: templateId={}, source={}", template.getId(), source);
                break;
            }
            if (!insertUserCoupon(template, memberId, grantBizId)) {
                // 本次预扣单独回补：DB 余量 +1 与 Redis +1 成对，且移出批量回补列表避免重复补
                String key = StockKeys.coupon(template.getId());
                couponTemplateMapper.restoreRemain(template.getId(), 1);
                deductedKeys.remove(key);
                safeRollbackRedis(key);
                continue;
            }
            granted++;
        }
        log.info("发券完成: templateId={}, source={}, granted={}", template.getId(), source, granted);
        return granted;
    }

    private boolean tryDeductStock(CouponTemplate template, List<String> deductedKeys) {
        String key = StockKeys.coupon(template.getId());
        if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            // key 缺失（首次发放 / 已过期 / Redis 重启）时按 DB 真实值重建，不从 0 起算
            CouponTemplate fresh = couponTemplateMapper.selectById(template.getId());
            int remain = fresh == null || fresh.getRemain() == null ? 0 : fresh.getRemain();
            stockService.reset(key, remain);
        }
        if (!stockService.tryDeduct(key, 1)) {
            return false;
        }
        if (couponTemplateMapper.deductRemain(template.getId()) == 0) {
            // DB 已领完：只补 Redis，DB 未发生变化，不能改 remain
            safeRollbackRedis(key);
            return false;
        }
        deductedKeys.add(key);
        return true;
    }

    private boolean insertUserCoupon(CouponTemplate template, Long memberId, String grantBizId) {
        LocalDateTime now = LocalDateTime.now();
        int validDays = template.getValidDays() == null || template.getValidDays() <= 0
                ? DEFAULT_VALID_DAYS : template.getValidDays();
        for (int attempt = 0; attempt < CouponConstants.CODE_SEQ_MAX_RETRY; attempt++) {
            UserCoupon userCoupon = new UserCoupon();
            userCoupon.setTemplateId(template.getId());
            userCoupon.setMemberId(memberId);
            userCoupon.setStoreId(template.getStoreId());
            userCoupon.setCodeSeq(nextCodeSeq());
            userCoupon.setStatus(0);
            userCoupon.setReceiveTime(now);
            userCoupon.setExpireAt(now.plusDays(validDays));
            userCoupon.setGrantBizId(grantBizId);
            try {
                userCouponMapper.insert(userCoupon);
            } catch (DuplicateKeyException e) {
                if (grantBizId != null && userCouponMapper.countByGrantBizId(grantBizId) > 0) {
                    // 规则发券的并发重放：唯一索引已存在同一幂等键，按「已发过」处理，由调用方回补库存
                    log.info("规则发券幂等键已存在，跳过: grantBizId={}", grantBizId);
                    return false;
                }
                log.warn("券码短序号冲突，重试: codeSeq={}", userCoupon.getCodeSeq());
                continue;
            }
            publishExpireMessage(userCoupon, template, defaultRemindDelay(template));
            return true;
        }
        log.error("券码短序号连续冲突，放弃本次发放: templateId={}, memberId={}",
                template.getId(), memberId);
        return false;
    }

    /**
     * 真实提醒的延迟 = 失效时间 − 提前量。延迟插件按 int 毫秒投递，超过 24.8 天会被钳到上限，
     * 更长的券期由 M11 的每小时兜底任务补齐。
     */
    private long defaultRemindDelay(CouponTemplate template) {
        long aheadMillis = TimeUnit.HOURS.toMillis(couponProperties.getExpireRemindAheadHours());
        long expireMillis = System.currentTimeMillis()
                + TimeUnit.DAYS.toMillis(template.getValidDays() == null ? DEFAULT_VALID_DAYS
                : template.getValidDays());
        return expireMillis - aheadMillis - System.currentTimeMillis();
    }

    /**
     * 券过期提醒：走延迟交换机到点触发，不占用轮询。
     */
    private void publishExpireMessage(UserCoupon userCoupon, CouponTemplate template, long delayMillis) {
        if (delayMillis <= 0L) {
            return;
        }
        long expireMillis = userCoupon.getExpireAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        CouponExpireMessage message = new CouponExpireMessage();
        message.setMessageId(newMessageId());
        message.setEventType(CouponExpireMessage.TYPE_EXPIRE_REMIND);
        message.setOccurredAtMillis(System.currentTimeMillis());
        message.setUserCouponId(userCoupon.getId());
        message.setTemplateId(userCoupon.getTemplateId());
        message.setTemplateName(template == null ? "" : template.getTemplateName());
        message.setMemberId(userCoupon.getMemberId());
        message.setStoreId(userCoupon.getStoreId());
        message.setExpireAtMillis(expireMillis);
        MqEnvelope envelope = new MqEnvelope();
        envelope.setMessageId(message.getMessageId());
        envelope.setExchangeName(RabbitConst.DELAY_EXCHANGE);
        envelope.setRoutingKey(RabbitConst.RK_COUPON_EXPIRE);
        envelope.setDelayMillis(delayMillis);
        envelope.setBody(message);
        messageProducer.publish(envelope);
    }

    private CouponTemplate requireIssuingTemplate(Long storeId, Long templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null || !storeId.equals(template.getStoreId())) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND);
        }
        if (!Integer.valueOf(CouponTemplateStatus.ISSUING.getCode()).equals(template.getStatus())) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_NOT_ISSUING);
        }
        return template;
    }

    private CouponTemplate resolveNewcomerTemplate(Long storeId) {
        Long templateId = parseTemplateId(couponProperties.getNewcomerTemplateId());
        if (templateId != null) {
            CouponTemplate template = couponTemplateMapper.selectById(templateId);
            if (template != null && storeId.equals(template.getStoreId())
                    && Integer.valueOf(CouponTemplateStatus.ISSUING.getCode()).equals(template.getStatus())) {
                return template;
            }
            log.warn("配置的新人券模板不可用，回退到名称匹配: templateId={}", templateId);
        }
        String name = couponProperties.getNewcomerTemplateName();
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return couponTemplateMapper.selectOne(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getStoreId, storeId)
                .eq(CouponTemplate::getTemplateName, name)
                .eq(CouponTemplate::getStatus, CouponTemplateStatus.ISSUING.getCode())
                .orderByDesc(CouponTemplate::getCreateTime)
                .last("LIMIT 1"));
    }

    /**
     * 事务回滚时把已预扣的 Redis 库存补回去；DB 由 Spring 回滚，两条路径不会重复回补。
     */
    private void registerRollbackCompensation(List<String> deductedKeys) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_ROLLED_BACK
                        && status != TransactionSynchronization.STATUS_UNKNOWN) {
                    return;
                }
                for (String key : deductedKeys) {
                    try {
                        stockService.rollback(key, 1, false);
                    } catch (RuntimeException e) {
                        log.error("券库存 Redis 回补失败，等待每日重建兜底: key={}", key, e);
                    }
                }
            }
        });
    }

    private void safeRollbackRedis(String key) {
        try {
            stockService.rollback(key, 1, false);
        } catch (RuntimeException e) {
            log.error("券库存 Redis 回补失败，等待每日重建兜底: key={}", key, e);
        }
    }

    private static Long parseTemplateId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            log.warn("comeoften.coupon.newcomer-template-id 不是合法数字，已忽略: {}", value);
            return null;
        }
    }

    private static String nextCodeSeq() {
        long value = ThreadLocalRandom.current().nextLong(CODE_SEQ_BOUND);
        return String.format(Locale.ROOT, "%06d", value);
    }

    private static String newMessageId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
