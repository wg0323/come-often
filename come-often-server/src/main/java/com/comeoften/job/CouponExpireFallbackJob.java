package com.comeoften.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.JobKeys;
import com.comeoften.common.job.JobLock;
import com.comeoften.common.job.JobResult;
import com.comeoften.config.CouponProperties;
import com.comeoften.module.coupon.constant.CouponTemplateStatus;
import com.comeoften.module.coupon.constant.UserCouponStatus;
import com.comeoften.module.coupon.entity.CouponTemplate;
import com.comeoften.module.coupon.entity.UserCoupon;
import com.comeoften.module.coupon.mapper.CouponTemplateMapper;
import com.comeoften.module.coupon.mapper.UserCouponMapper;
import com.comeoften.mq.message.CouponExpireMessage;
import com.comeoften.mq.service.NotifyRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 券即将过期提醒的小时级兜底（计划 3.10）。实时提醒由领券时投递的延迟消息承担，
 * 但延迟插件按 int 毫秒投递、单条上限约 24.8 天（M5 差异 48），更长有效期的券会提前触发，
 * 本任务每小时补一次「即将过期但还没提醒过」的券。
 *
 * <p><b>与延迟消息共用同一把去重键</b> {@code comeoften:job:coupon-expire:{userCouponId}}
 * （{@code CouponExpireListener} 也写这个键）：两条链路都落 {@code message_record(COUPON_EXPIRE)}，
 * 各用一套键会让同一张券被提醒两次。TTL 到券失效后 1 小时，Redis 清空后最坏重复提醒一次，
 * 不会重复落业务数据。</p>
 *
 * <p>分页扫描：默认 24 小时窗口 + 单批 200 条时，若窗口内待提醒券多于单批条数，
 * 只按 expireAt 升序取一批会让后面的券永远排在已提醒的那批之后。这里按 expireAt 游标最多翻
 * {@value #MAX_PAGES} 页，用 id 去重，保证同一轮能推进。</p>
 *
 * <p>与延迟消息共用同一条落库出口（{@link NotifyRecordService#saveCouponExpireNotice}），
 * 通道仍是 LOG，真实订阅消息要等模板 ID 与一次性订阅授权。</p>
 */
@Slf4j
@Component
public class CouponExpireFallbackJob {

    public static final String JOB_NAME = "coupon-expire-fallback";

    private static final long LOCK_TTL_SECONDS = 600L;

    private static final long EXTRA_TTL_SECONDS = 3600L;

    private static final int MAX_PAGES = 5;

    private final UserCouponMapper userCouponMapper;

    private final CouponTemplateMapper couponTemplateMapper;

    private final NotifyRecordService notifyRecordService;

    private final StringRedisTemplate stringRedisTemplate;

    private final CouponProperties couponProperties;

    private final int batchSize;

    public CouponExpireFallbackJob(UserCouponMapper userCouponMapper,
                                   CouponTemplateMapper couponTemplateMapper,
                                   NotifyRecordService notifyRecordService,
                                   StringRedisTemplate stringRedisTemplate,
                                   CouponProperties couponProperties,
                                   @Value("${comeoften.coupon.expire-fallback-batch-size:200}") int batchSize) {
        this.userCouponMapper = userCouponMapper;
        this.couponTemplateMapper = couponTemplateMapper;
        this.notifyRecordService = notifyRecordService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.couponProperties = couponProperties;
        this.batchSize = batchSize;
    }

    /**
     * 补发一次即将过期提醒，返回本次新提醒的券数。
     */
    @JobLock(key = JOB_NAME, ttlSeconds = LOCK_TTL_SECONDS)
    public JobResult remind() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusHours(couponProperties.getExpireRemindAheadHours());
        Set<Long> seen = new HashSet<>();
        LocalDateTime cursor = now;
        int candidates = 0;
        int reminded = 0;
        for (int page = 0; page < MAX_PAGES; page++) {
            List<UserCoupon> batch = selectBatch(cursor, deadline);
            if (batch.isEmpty()) {
                break;
            }
            candidates += batch.size();
            Map<Long, CouponTemplate> templates = loadTemplates(batch);
            boolean advanced = false;
            for (UserCoupon coupon : batch) {
                if (!seen.add(coupon.getId())) {
                    continue;
                }
                advanced = true;
                CouponTemplate template = templates.get(coupon.getTemplateId());
                if (template == null
                        || !Integer.valueOf(CouponTemplateStatus.ISSUING.getCode()).equals(template.getStatus())) {
                    continue;
                }
                if (remind(coupon, template, now)) {
                    reminded++;
                }
            }
            if (!advanced) {
                break;
            }
            cursor = batch.get(batch.size() - 1).getExpireAt();
        }
        log.info("券过期提醒兜底完成: candidates={}, reminded={}", candidates, reminded);
        return JobResult.executed(JOB_NAME, reminded, "candidates=" + candidates);
    }

    private List<UserCoupon> selectBatch(LocalDateTime cursor, LocalDateTime deadline) {
        return userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getStatus, UserCouponStatus.AVAILABLE.getCode())
                .ge(UserCoupon::getExpireAt, cursor)
                .le(UserCoupon::getExpireAt, deadline)
                .orderByAsc(UserCoupon::getExpireAt)
                .last("LIMIT " + batchSize));
    }

    private boolean remind(UserCoupon coupon, CouponTemplate template, LocalDateTime now) {
        String key = JobKeys.couponExpireReminded(coupon.getId());
        long ttlSeconds = Duration.between(now, coupon.getExpireAt()).getSeconds() + EXTRA_TTL_SECONDS;
        Boolean first = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", ttlSeconds, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(first)) {
            return false;
        }
        try {
            notifyRecordService.saveCouponExpireNotice(buildMessage(coupon, template));
            return true;
        } catch (RuntimeException e) {
            // 先写标记再落库，落库失败必须把标记删掉，否则这张券到过期都不会再被提醒
            log.error("券过期提醒落库失败，已回滚去重标记: userCouponId={}", coupon.getId(), e);
            stringRedisTemplate.delete(key);
            return false;
        }
    }

    private static CouponExpireMessage buildMessage(UserCoupon coupon, CouponTemplate template) {
        CouponExpireMessage message = new CouponExpireMessage();
        message.setMessageId("job-coupon-expire-" + coupon.getId());
        message.setUserCouponId(coupon.getId());
        message.setTemplateId(template.getId());
        message.setTemplateName(template.getTemplateName());
        message.setMemberId(coupon.getMemberId());
        message.setStoreId(coupon.getStoreId());
        message.setExpireAtMillis(coupon.getExpireAt()
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        return message;
    }

    private Map<Long, CouponTemplate> loadTemplates(List<UserCoupon> coupons) {
        List<Long> ids = coupons.stream()
                .map(UserCoupon::getTemplateId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, CouponTemplate> result = new HashMap<>(ids.size());
        if (ids.isEmpty()) {
            return result;
        }
        result.putAll(couponTemplateMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(CouponTemplate::getId, Function.identity(), (left, right) -> left)));
        return result;
    }
}
