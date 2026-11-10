package com.comeoften.module.bundle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.PageResult;
import com.comeoften.module.bundle.constant.BundleStatus;
import com.comeoften.module.bundle.converter.BundleConverter;
import com.comeoften.module.bundle.dto.BundleSaveDTO;
import com.comeoften.module.bundle.entity.GroupBundle;
import com.comeoften.module.bundle.entity.GroupBundleItem;
import com.comeoften.module.bundle.mapper.GroupBundleItemMapper;
import com.comeoften.module.bundle.mapper.GroupBundleMapper;
import com.comeoften.module.bundle.service.GroupBundleService;
import com.comeoften.module.bundle.vo.BundleVO;
import com.comeoften.module.dish.entity.Dish;
import com.comeoften.module.dish.mapper.DishMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 套餐实现。套餐是独立商品：金额只认 bundle_price，不把明细菜品拿去二次计价；
 * 明细只用于展示与下单时的行摘要快照。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupBundleServiceImpl implements GroupBundleService {

    private static final long DEFAULT_PAGE_SIZE = 10L;

    private static final long MAX_PAGE_SIZE = 50L;

    private static final int SCALE = 2;

    private final GroupBundleMapper groupBundleMapper;

    private final GroupBundleItemMapper groupBundleItemMapper;

    private final DishMapper dishMapper;

    @Override
    public List<BundleVO> listOnShelf(Long storeId) {
        List<GroupBundle> bundles = groupBundleMapper.selectList(new LambdaQueryWrapper<GroupBundle>()
                .eq(GroupBundle::getStoreId, storeId)
                .eq(GroupBundle::getStatus, BundleStatus.ON_SHELF.getCode())
                .gt(GroupBundle::getRemainStock, 0)
                .orderByDesc(GroupBundle::getCreateTime));
        if (bundles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = bundles.stream().map(GroupBundle::getId).collect(Collectors.toList());
        Map<Long, List<GroupBundleItem>> itemsByBundle = loadItems(ids);
        return bundles.stream().map(bundle -> BundleConverter.toVO(bundle,
                        itemsByBundle.getOrDefault(bundle.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Override
    public BundleVO detail(Long storeId, Long bundleId, boolean requireOnShelf) {
        GroupBundle bundle = require(storeId, bundleId);
        if (requireOnShelf) {
            assertOnShelf(bundle);
        }
        return BundleConverter.toVO(bundle, groupBundleItemMapper.selectByBundle(bundleId));
    }

    @Override
    public PageResult<BundleVO> pageForAdmin(Long storeId, Long current, Long size,
                                              Integer status, String keyword) {
        long pageNo = current == null || current < 1 ? 1L : current;
        long pageSize = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        IPage<GroupBundle> page = groupBundleMapper.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<GroupBundle>()
                        .eq(GroupBundle::getStoreId, storeId)
                        .eq(status != null, GroupBundle::getStatus, status)
                        .likeRight(StringUtils.hasText(keyword), GroupBundle::getBundleName, keyword)
                        .orderByDesc(GroupBundle::getCreateTime));
        List<GroupBundle> records = page.getRecords();
        Map<Long, List<GroupBundleItem>> itemsByBundle = records.isEmpty() ? Collections.emptyMap()
                : loadItems(records.stream().map(GroupBundle::getId).collect(Collectors.toList()));
        Map<Long, List<GroupBundleItem>> finalItems = itemsByBundle;
        List<BundleVO> vos = records.stream()
                .map(bundle -> BundleConverter.toVO(bundle,
                        finalItems.getOrDefault(bundle.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(Long storeId, BundleSaveDTO dto) {
        GroupBundle current = dto.getId() == null ? null : require(storeId, dto.getId());
        BigDecimal price = dto.getBundlePrice().setScale(SCALE, RoundingMode.HALF_UP);
        if (dto.getOriginalPrice() != null && dto.getOriginalPrice().compareTo(price) < 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "划线价不能低于套餐售价");
        }
        BundleStatus status = dto.getStatus() == null ? null : BundleStatus.of(dto.getStatus());
        if (dto.getStatus() != null && status == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "套餐状态只允许 0 或 1");
        }
        List<Dish> dishes = resolveDishes(storeId, dto.getItems());
        GroupBundle bundle = new GroupBundle();
        bundle.setStoreId(storeId);
        bundle.setBundleName(dto.getBundleName().trim());
        bundle.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : "");
        bundle.setBundlePrice(price);
        bundle.setOriginalPrice(dto.getOriginalPrice());
        bundle.setTotalStock(dto.getTotalStock());
        // 编辑时未提交状态保持原值，与 DishServiceImpl.save 的口径一致，避免静默下架
        bundle.setStatus(status == null
                ? (current == null ? BundleStatus.OFF_SHELF.getCode() : current.getStatus())
                : status.getCode());
        if (current == null) {
            bundle.setRemainStock(dto.getTotalStock());
            groupBundleMapper.insert(bundle);
        } else {
            // 已售份数不随编辑变化：改总库存时按比例平移余量，避免凭空补货或负库存
            int sold = Math.max(0, current.getTotalStock() - current.getRemainStock());
            bundle.setId(current.getId());
            bundle.setRemainStock(Math.max(0, dto.getTotalStock() - sold));
            groupBundleMapper.updateById(bundle);
        }
        replaceItems(bundle.getId(), dto.getItems(), dishes);
        log.info("团购套餐已保存: bundleId={}, storeId={}, items={}, status={}",
                bundle.getId(), storeId, dto.getItems().size(), bundle.getStatus());
        return bundle.getId();
    }

    @Override
    public void updateStatus(Long storeId, Long bundleId, Integer status) {
        require(storeId, bundleId);
        BundleStatus target = BundleStatus.of(status);
        if (target == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "套餐状态只允许 0 或 1");
        }
        GroupBundle patch = new GroupBundle();
        patch.setId(bundleId);
        patch.setStatus(target.getCode());
        groupBundleMapper.updateById(patch);
        log.info("团购套餐上下架: bundleId={}, status={}", bundleId, target.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long storeId, Long bundleId) {
        require(storeId, bundleId);
        groupBundleItemMapper.deleteByBundle(bundleId);
        groupBundleMapper.deleteById(bundleId);
    }

    @Override
    public BundleVO requireAndDeduct(Long storeId, Long bundleId, int quantity) {
        GroupBundle bundle = require(storeId, bundleId);
        assertOnShelf(bundle);
        List<GroupBundleItem> items = groupBundleItemMapper.selectByBundle(bundleId);
        if (items.isEmpty()) {
            throw new BizException(ErrorCode.GROUP_BUNDLE_ITEM_INVALID);
        }
        if (groupBundleMapper.deductStock(bundleId, quantity) == 0) {
            throw new BizException(ErrorCode.GROUP_BUNDLE_SOLD_OUT);
        }
        return BundleConverter.toVO(bundle, items);
    }

    @Override
    public void restoreStock(Long bundleId, int quantity) {
        groupBundleMapper.restoreStock(bundleId, quantity);
    }

    // ------------------------------------------------------------------ 私有逻辑

    private GroupBundle require(Long storeId, Long bundleId) {
        if (bundleId == null) {
            throw new BizException(ErrorCode.GROUP_BUNDLE_NOT_FOUND);
        }
        GroupBundle bundle = groupBundleMapper.selectById(bundleId);
        if (bundle == null || !storeId.equals(bundle.getStoreId())) {
            throw new BizException(ErrorCode.GROUP_BUNDLE_NOT_FOUND);
        }
        return bundle;
    }

    private static void assertOnShelf(GroupBundle bundle) {
        if (!BundleStatus.isOnShelf(bundle.getStatus())) {
            throw new BizException(ErrorCode.GROUP_BUNDLE_OFF_SHELF);
        }
        if (bundle.getRemainStock() == null || bundle.getRemainStock() <= 0) {
            throw new BizException(ErrorCode.GROUP_BUNDLE_SOLD_OUT);
        }
    }

    /**
     * 明细菜品必须属于本店；允许菜品本身下架（套餐独立售卖），但不允许重复菜品或空明细。
     */
    private List<Dish> resolveDishes(Long storeId, List<BundleSaveDTO.ItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new BizException(ErrorCode.GROUP_BUNDLE_ITEM_INVALID);
        }
        Set<Long> dishIds = new HashSet<>();
        for (BundleSaveDTO.ItemDTO item : items) {
            if (item.getDishId() == null || !dishIds.add(item.getDishId())) {
                throw new BizException(ErrorCode.GROUP_BUNDLE_ITEM_INVALID, "套餐明细菜品重复或为空");
            }
        }
        List<Dish> dishes = dishMapper.selectBatchIds(new ArrayList<>(dishIds));
        if (dishes.size() != dishIds.size()
                || dishes.stream().anyMatch(dish -> !Objects.equals(dish.getStoreId(), storeId))) {
            throw new BizException(ErrorCode.GROUP_BUNDLE_ITEM_INVALID, "套餐明细菜品不存在或不属于本店");
        }
        return dishes;
    }

    /**
     * 明细整体覆盖：物理删除旧行再按提交顺序写入。uk_bundle_dish 不含 is_deleted，
     * 若用逻辑删除 + 重新插入会撞唯一键，所以配置型从表直接重建。
     */
    private void replaceItems(Long bundleId, List<BundleSaveDTO.ItemDTO> items, List<Dish> dishes) {
        groupBundleItemMapper.deleteByBundle(bundleId);
        Map<Long, Dish> dishMap = dishes.stream()
                .collect(Collectors.toMap(Dish::getId, Function.identity(), (left, right) -> left));
        for (int index = 0; index < items.size(); index++) {
            BundleSaveDTO.ItemDTO row = items.get(index);
            Dish dish = dishMap.get(row.getDishId());
            GroupBundleItem item = new GroupBundleItem();
            item.setBundleId(bundleId);
            item.setDishId(row.getDishId());
            item.setDishName(dish.getDishName());
            item.setQuantity(row.getQuantity() == null ? 1 : row.getQuantity());
            item.setSort(index);
            groupBundleItemMapper.insert(item);
        }
    }

    private Map<Long, List<GroupBundleItem>> loadItems(List<Long> bundleIds) {
        return groupBundleItemMapper.selectList(new LambdaQueryWrapper<GroupBundleItem>()
                        .in(GroupBundleItem::getBundleId, bundleIds)
                        .orderByAsc(GroupBundleItem::getSort)
                        .orderByAsc(GroupBundleItem::getId))
                .stream().collect(Collectors.groupingBy(GroupBundleItem::getBundleId));
    }
}
