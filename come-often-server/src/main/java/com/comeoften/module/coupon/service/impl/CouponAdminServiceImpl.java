package com.comeoften.module.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.PageResult;
import com.comeoften.common.StockKeys;
import com.comeoften.module.auth.entity.SysUser;
import com.comeoften.module.auth.mapper.SysUserMapper;
import com.comeoften.module.coupon.constant.CouponTemplateStatus;
import com.comeoften.module.coupon.constant.CouponType;
import com.comeoften.module.coupon.converter.CouponConverter;
import com.comeoften.module.coupon.dto.CouponGrantDTO;
import com.comeoften.module.coupon.dto.CouponTemplateSaveDTO;
import com.comeoften.module.coupon.entity.CouponTemplate;
import com.comeoften.module.coupon.entity.CouponTemplateDish;
import com.comeoften.module.coupon.entity.CouponUseRecord;
import com.comeoften.module.coupon.entity.UserCoupon;
import com.comeoften.module.coupon.mapper.CouponTemplateDishMapper;
import com.comeoften.module.coupon.mapper.CouponTemplateMapper;
import com.comeoften.module.coupon.mapper.CouponUseRecordMapper;
import com.comeoften.module.coupon.mapper.UserCouponMapper;
import com.comeoften.module.coupon.service.CouponAdminService;
import com.comeoften.module.coupon.service.CouponGrantService;
import com.comeoften.module.coupon.vo.CouponOperatorVO;
import com.comeoften.module.coupon.vo.CouponRoiVO;
import com.comeoften.module.coupon.vo.CouponTemplateVO;
import com.comeoften.module.coupon.vo.CouponUseRecordVO;
import com.comeoften.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 营销管理实现。DB 是库存最终事实源：模板变更在同一事务写 DB，提交后重置 Redis 库存键，
 * 避免事务回滚时留下 Redis 新值。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponAdminServiceImpl implements CouponAdminService {

    private static final int DEFAULT_VALID_DAYS = 30;

    private static final int DEFAULT_STACKABLE = 0;

    private static final long PAGE_MAX_SIZE = 100L;

    private static final BigDecimal FULL_RATE = new BigDecimal("1.00");

    private final CouponTemplateMapper couponTemplateMapper;

    private final CouponTemplateDishMapper couponTemplateDishMapper;

    private final UserCouponMapper userCouponMapper;

    private final CouponUseRecordMapper couponUseRecordMapper;

    private final CouponGrantService couponGrantService;

    private final StockService stockService;

    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<CouponTemplateVO> pageTemplate(Long storeId, Long current, Long size,
                                                     String keyword, Integer status) {
        Page<CouponTemplate> page = new Page<>(pageCurrent(current), pageSize(size));
        IPage<CouponTemplate> result = couponTemplateMapper.selectPage(page,
                new LambdaQueryWrapper<CouponTemplate>()
                        .eq(CouponTemplate::getStoreId, storeId)
                        .eq(status != null, CouponTemplate::getStatus, status)
                        .like(StringUtils.hasText(keyword), CouponTemplate::getTemplateName, keyword)
                        .orderByDesc(CouponTemplate::getCreateTime));
        Map<Long, List<Long>> dishes = loadDishRelations(result.getRecords().stream()
                .map(CouponTemplate::getId).collect(Collectors.toList()));
        List<CouponTemplateVO> records = new ArrayList<>();
        for (CouponTemplate entity : result.getRecords()) {
            records.add(CouponConverter.toTemplateVO(entity,
                    dishes.getOrDefault(entity.getId(), Collections.emptyList())));
        }
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public CouponTemplateVO detailTemplate(Long storeId, Long templateId) {
        CouponTemplate template = requireTemplate(storeId, templateId);
        return CouponConverter.toTemplateVO(template, loadDishIds(template.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponTemplateVO createTemplate(Long storeId, CouponTemplateSaveDTO dto) {
        validate(dto);
        CouponTemplate template = new CouponTemplate();
        template.setStoreId(storeId);
        applyFields(template, dto);
        template.setTotal(dto.getTotal());
        template.setRemain(dto.getTotal());
        template.setStatus(dto.getStatus() == null
                ? CouponTemplateStatus.ISSUING.getCode() : dto.getStatus());
        template.setIsDeleted(0);
        couponTemplateMapper.insert(template);
        replaceDishRelations(template.getId(), dto);
        resetStockAfterCommit(template.getId(), template.getRemain());
        log.info("券模板已创建: templateId={}, name={}, type={}, total={}", template.getId(),
                template.getTemplateName(), template.getCouponType(), template.getTotal());
        return CouponConverter.toTemplateVO(template, loadDishIds(template.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponTemplateVO updateTemplate(Long storeId, Long templateId, CouponTemplateSaveDTO dto) {
        validate(dto);
        CouponTemplate template = requireTemplate(storeId, templateId);
        int oldTotal = template.getTotal() == null ? 0 : template.getTotal();
        int oldRemain = template.getRemain() == null ? 0 : template.getRemain();
        applyFields(template, dto);
        // 已领走的部分不能被编辑覆盖掉：新余量 = 新总量 − 已领取量
        int consumed = Math.max(0, oldTotal - oldRemain);
        int remain = Math.max(0, dto.getTotal() - consumed);
        template.setTotal(dto.getTotal());
        template.setRemain(remain);
        if (dto.getStatus() != null) {
            template.setStatus(dto.getStatus());
        }
        couponTemplateMapper.updateById(template);
        replaceDishRelations(template.getId(), dto);
        resetStockAfterCommit(template.getId(), remain);
        log.info("券模板已更新: templateId={}, total={}, remain={}", templateId, dto.getTotal(), remain);
        return CouponConverter.toTemplateVO(template, loadDishIds(template.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponTemplateVO updateStatus(Long storeId, Long templateId, Integer status) {
        if (CouponTemplateStatus.of(status) == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "券模板状态不合法");
        }
        CouponTemplate template = requireTemplate(storeId, templateId);
        couponTemplateMapper.update(null, new LambdaUpdateWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, templateId)
                .set(CouponTemplate::getStatus, status));
        template.setStatus(status);
        resetStockAfterCommit(templateId, template.getRemain());
        log.info("券模板状态已切换: templateId={}, status={}", templateId, status);
        return CouponConverter.toTemplateVO(template, loadDishIds(templateId));
    }

    @Override
    public int grant(Long storeId, CouponGrantDTO dto) {
        return couponGrantService.grant(storeId, dto.getTemplateId(), dto.getMemberIds(), "ADMIN");
    }

    @Override
    public PageResult<CouponUseRecordVO> pageUseRecord(Long storeId, Long current, Long size) {
        return pageUseRecord(storeId, current, size, null, null);
    }

    @Override
    public PageResult<CouponUseRecordVO> pageUseRecord(Long storeId, Long current, Long size,
                                                       Long operatorUserId, LocalDate verifyDate) {
        Page<CouponUseRecord> page = new Page<>(pageCurrent(current), pageSize(size));
        LocalDateTime from = verifyDate == null ? null : verifyDate.atStartOfDay();
        LocalDateTime to = verifyDate == null ? null : verifyDate.plusDays(1L).atStartOfDay();
        IPage<CouponUseRecord> result = couponUseRecordMapper.selectPage(page,
                new LambdaQueryWrapper<CouponUseRecord>()
                        .eq(CouponUseRecord::getStoreId, storeId)
                        .eq(operatorUserId != null, CouponUseRecord::getOperatorUserId, operatorUserId)
                        .ge(from != null, CouponUseRecord::getVerifyTime, from)
                        .lt(to != null, CouponUseRecord::getVerifyTime, to)
                        .orderByDesc(CouponUseRecord::getVerifyTime));
        List<CouponUseRecord> records = result.getRecords();
        Map<Long, UserCoupon> coupons = loadCoupons(records.stream()
                .map(CouponUseRecord::getUserCouponId).distinct().collect(Collectors.toList()));
        Map<Long, CouponTemplate> templates = loadTemplates(coupons.values().stream()
                .map(UserCoupon::getTemplateId).distinct().collect(Collectors.toList()));
        Map<Long, String> operators = loadOperatorNames(records.stream()
                .map(CouponUseRecord::getOperatorUserId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList()));
        List<CouponUseRecordVO> vos = new ArrayList<>();
        for (CouponUseRecord record : records) {
            UserCoupon coupon = coupons.get(record.getUserCouponId());
            CouponTemplate template = coupon == null ? null : templates.get(coupon.getTemplateId());
            vos.add(CouponConverter.toUseRecordVO(record,
                    template == null ? "" : template.getTemplateName(),
                    template == null ? null : template.getCouponType(),
                    operators.get(record.getOperatorUserId())));
        }
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), vos);
    }

    @Override
    public List<CouponOperatorVO> listRecordOperators(Long storeId, LocalDate verifyDate) {
        LocalDate date = verifyDate == null ? LocalDate.now() : verifyDate;
        List<CouponUseRecord> records = couponUseRecordMapper.selectList(
                new LambdaQueryWrapper<CouponUseRecord>()
                        .eq(CouponUseRecord::getStoreId, storeId)
                        .ge(CouponUseRecord::getVerifyTime, date.atStartOfDay())
                        .lt(CouponUseRecord::getVerifyTime, date.plusDays(1L).atStartOfDay())
                        .isNotNull(CouponUseRecord::getOperatorUserId));
        List<Long> operatorIds = records.stream().map(CouponUseRecord::getOperatorUserId).distinct()
                .collect(Collectors.toList());
        Map<Long, String> names = loadOperatorNames(operatorIds);
        List<CouponOperatorVO> operators = new ArrayList<>();
        for (Long operatorId : operatorIds) {
            CouponOperatorVO vo = new CouponOperatorVO();
            vo.setOperatorUserId(operatorId);
            vo.setOperatorName(names.getOrDefault(operatorId, ""));
            operators.add(vo);
        }
        return operators;
    }

    @Override
    public List<CouponRoiVO> roi(Long storeId, Long templateId) {
        return couponTemplateMapper.roiByStore(storeId, templateId).stream()
                .map(CouponConverter::toRoiVO)
                .collect(Collectors.toList());
    }

    @Override
    public void resendExpireRemind(Long storeId, Long userCouponId, long delaySeconds) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !storeId.equals(userCoupon.getStoreId())) {
            throw new BizException(ErrorCode.COUPON_NOT_FOUND);
        }
        couponGrantService.resendExpireRemind(userCouponId, delaySeconds);
        log.info("已补发券过期提醒: userCouponId={}, delaySeconds={}", userCouponId, delaySeconds);
    }

    private void applyFields(CouponTemplate template, CouponTemplateSaveDTO dto) {
        template.setTemplateName(dto.getTemplateName());
        template.setCouponType(dto.getCouponType());
        template.setFaceValue(dto.getFaceValue() == null ? BigDecimal.ZERO : dto.getFaceValue());
        template.setDiscountRate(dto.getDiscountRate());
        template.setThresholdAmount(dto.getThresholdAmount() == null
                ? BigDecimal.ZERO : dto.getThresholdAmount());
        template.setDishId(dto.getDishId());
        template.setValidDays(dto.getValidDays() == null || dto.getValidDays() <= 0
                ? DEFAULT_VALID_DAYS : dto.getValidDays());
        template.setStartTime(dto.getStartTime());
        template.setEndTime(dto.getEndTime());
        template.setStackable(dto.getStackable() == null ? DEFAULT_STACKABLE : dto.getStackable());
    }

    /**
     * 按券类型校验必填项：面额型必有面额，折扣券必有折扣率，单品券必指定菜品。
     */
    private void validate(CouponTemplateSaveDTO dto) {
        CouponType type = CouponType.of(dto.getCouponType());
        if (type == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "券类型不合法：" + dto.getCouponType());
        }
        if (type == CouponType.DISCOUNT) {
            BigDecimal rate = dto.getDiscountRate();
            if (rate == null || rate.signum() <= 0 || rate.compareTo(FULL_RATE) >= 0) {
                throw new BizException(ErrorCode.PARAM_INVALID, "折扣率必须在 0 与 1 之间");
            }
            return;
        }
        BigDecimal face = dto.getFaceValue();
        if (face == null || face.signum() <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "面额必须大于 0");
        }
        if (type == CouponType.DISH && dto.getDishId() == null
                && (dto.getDishIds() == null || dto.getDishIds().isEmpty())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "单品券必须指定菜品");
        }
    }

    private void replaceDishRelations(Long templateId, CouponTemplateSaveDTO dto) {
        // uk_template_dish 不含 is_deleted，逻辑删除后重建同名关联会撞索引，关联表直接物理清理
        couponTemplateDishMapper.delete(new LambdaQueryWrapper<CouponTemplateDish>()
                .eq(CouponTemplateDish::getTemplateId, templateId));
        List<Long> dishIds = new ArrayList<>();
        if (dto.getDishId() != null) {
            dishIds.add(dto.getDishId());
        }
        if (dto.getDishIds() != null) {
            dishIds.addAll(dto.getDishIds());
        }
        for (Long dishId : dishIds.stream().distinct().collect(Collectors.toList())) {
            if (dishId == null) {
                continue;
            }
            CouponTemplateDish relation = new CouponTemplateDish();
            relation.setTemplateId(templateId);
            relation.setDishId(dishId);
            relation.setIsDeleted(0);
            couponTemplateDishMapper.insert(relation);
        }
    }

    private CouponTemplate requireTemplate(Long storeId, Long templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null || !storeId.equals(template.getStoreId())) {
            throw new BizException(ErrorCode.COUPON_TEMPLATE_NOT_FOUND);
        }
        return template;
    }

    private List<Long> loadDishIds(Long templateId) {
        return couponTemplateDishMapper.selectList(new LambdaQueryWrapper<CouponTemplateDish>()
                        .eq(CouponTemplateDish::getTemplateId, templateId))
                .stream().map(CouponTemplateDish::getDishId).collect(Collectors.toList());
    }

    private Map<Long, List<Long>> loadDishRelations(List<Long> templateIds) {
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

    private Map<Long, UserCoupon> loadCoupons(List<Long> userCouponIds) {
        if (userCouponIds == null || userCouponIds.isEmpty()) {
            return new HashMap<>(0);
        }
        return userCouponMapper.selectBatchIds(userCouponIds).stream()
                .collect(Collectors.toMap(UserCoupon::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, CouponTemplate> loadTemplates(List<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return new HashMap<>(0);
        }
        return couponTemplateMapper.selectBatchIds(templateIds).stream()
                .collect(Collectors.toMap(CouponTemplate::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, String> loadOperatorNames(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>(0);
        }
        return sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        user -> user.getRealName() == null ? "" : user.getRealName(),
                        (left, right) -> left));
    }

    /**
     * 事务提交后重置 Redis 库存键，回滚时不会留下新的 Redis 值。
     */
    private void resetStockAfterCommit(Long templateId, Integer remain) {
        long value = remain == null ? 0L : remain;
        String key = StockKeys.coupon(templateId);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            stockService.reset(key, value);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                stockService.reset(key, value);
            }
        });
    }

    private static long pageCurrent(Long current) {
        return Math.max(1L, current == null ? 1L : current);
    }

    private static long pageSize(Long size) {
        return Math.min(Math.max(1L, size == null ? 10L : size), PAGE_MAX_SIZE);
    }
}
