package com.comeoften.module.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.PageResult;
import com.comeoften.integration.storage.FileStorageService;
import com.comeoften.module.dish.converter.DishListAssembler;
import com.comeoften.module.dish.dto.DishSaveDTO;
import com.comeoften.module.dish.dto.DishStockDTO;
import com.comeoften.module.dish.entity.Dish;
import com.comeoften.module.dish.entity.DishAddon;
import com.comeoften.module.dish.entity.DishCategory;
import com.comeoften.module.dish.entity.DishImage;
import com.comeoften.module.dish.entity.DishSpec;
import com.comeoften.module.dish.entity.DishSpecOption;
import com.comeoften.module.dish.entity.DishStock;
import com.comeoften.module.dish.entity.DishTag;
import com.comeoften.module.dish.entity.DishTagRel;
import com.comeoften.module.dish.mapper.DishAddonMapper;
import com.comeoften.module.dish.mapper.DishCategoryMapper;
import com.comeoften.module.dish.mapper.DishImageMapper;
import com.comeoften.module.dish.mapper.DishMapper;
import com.comeoften.module.dish.mapper.DishSpecMapper;
import com.comeoften.module.dish.mapper.DishSpecOptionMapper;
import com.comeoften.module.dish.mapper.DishStockMapper;
import com.comeoften.module.dish.mapper.DishTagMapper;
import com.comeoften.module.dish.mapper.DishTagRelMapper;
import com.comeoften.module.dish.service.DishQueryService;
import com.comeoften.module.dish.service.DishService;
import com.comeoften.module.dish.service.DishStockService;
import com.comeoften.module.dish.util.DishConstants;
import com.comeoften.module.dish.vo.DishDetailVO;
import com.comeoften.module.dish.vo.DishListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 后台菜品管理。规格、加料、图片采用整体覆盖；标签走差量同步，避免与唯一索引冲突。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    private static final int DEFAULT_STOCK_TOTAL = DishConstants.DEFAULT_STOCK_TOTAL;

    private final DishMapper dishMapper;

    private final DishCategoryMapper dishCategoryMapper;

    private final DishImageMapper dishImageMapper;

    private final DishSpecMapper dishSpecMapper;

    private final DishSpecOptionMapper dishSpecOptionMapper;

    private final DishAddonMapper dishAddonMapper;

    private final DishTagRelMapper dishTagRelMapper;

    private final DishTagMapper dishTagMapper;

    private final DishStockMapper dishStockMapper;

    private final DishListAssembler dishListAssembler;

    private final DishQueryService dishQueryService;

    private final DishStockService dishStockService;

    @Override
    public PageResult<DishListVO> page(Long storeId, Long current, Long size, String keyword,
                                       Long categoryId, Integer status) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStoreId, storeId);
        if (StringUtils.hasText(keyword)) {
            wrapper.likeRight(Dish::getDishName, keyword.trim());
        }
        if (categoryId != null) {
            wrapper.eq(Dish::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Dish::getStatus, status);
        }
        wrapper.orderByAsc(Dish::getSort).orderByAsc(Dish::getId);
        IPage<Dish> page = dishMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(),
                dishListAssembler.assemble(storeId, page.getRecords()));
    }

    @Override
    public DishDetailVO detailForAdmin(Long storeId, Long dishId) {
        return dishQueryService.detailForAdmin(storeId, dishId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(Long storeId, DishSaveDTO dto) {
        DishCategory category = dishCategoryMapper.selectById(dto.getCategoryId());
        if (category == null || !Objects.equals(category.getStoreId(), storeId)) {
            throw new BizException(ErrorCode.DISH_CATEGORY_NOT_FOUND);
        }
        boolean isNew = dto.getId() == null;
        // 编辑时以库中现有值为基线：未提交的字段保持原值，避免静默下架或清掉预定菜标记
        Dish current = isNew ? null : require(storeId, dto.getId());
        Dish dish = new Dish();
        BeanUtils.copyProperties(dto, dish);
        dish.setStoreId(storeId);
        dish.setIsPreorder(dto.getPreorder() == null
                ? (current == null ? DishConstants.NO : current.getIsPreorder())
                : (dto.getPreorder() ? DishConstants.YES : DishConstants.NO));
        dish.setPreorderHours(DishConstants.PREORDER_HOURS);
        dish.setStatus(dto.getStatus() == null
                ? (current == null ? DishConstants.STATUS_OFF_SHELF : current.getStatus())
                : dto.getStatus());
        dish.setSort(dto.getSort() == null ? (current == null ? 0 : current.getSort()) : dto.getSort());
        dish.setMonthlySales(dto.getMonthlySales() == null
                ? (current == null ? 0 : current.getMonthlySales()) : dto.getMonthlySales());
        dish.setOriginalPrice(dto.getOriginalPrice() == null
                ? (current == null ? null : current.getOriginalPrice()) : dto.getOriginalPrice());
        dish.setMemberPrice(resolveMemberPrice(dto, current, dish.getPrice()));
        dish.setMemberOnly(dto.getMemberOnly() == null
                ? (current == null ? DishConstants.NO : current.getMemberOnly())
                : (dto.getMemberOnly() ? DishConstants.YES : DishConstants.NO));
        dish.setSubtitle(valueOrFallback(dto.getSubtitle(), current == null ? "" : current.getSubtitle()));
        dish.setDescription(valueOrFallback(dto.getDescription(), current == null ? "" : current.getDescription()));
        dish.setAllergen(valueOrFallback(dto.getAllergen(), current == null ? "" : current.getAllergen()));
        dish.setCraftInfo(valueOrFallback(dto.getCraftInfo(), current == null ? "" : current.getCraftInfo()));
        if (isNew) {
            dishMapper.insert(dish);
        } else {
            dishMapper.updateById(dish);
        }
        Long dishId = dish.getId();
        replaceChildren(dishId, dto);
        if (dto.getTagIds() != null) {
            syncTags(dishId, dto.getTagIds());
        }
        if (isNew) {
            // 新建菜品一定建立当日库存与 Redis key，否则 M3 的 Lua 预扣会把不存在的 key 当作 0
            DishStockDTO stock = new DishStockDTO();
            stock.setDishId(dishId);
            stock.setTotal(dto.getStockTotal() == null ? DEFAULT_STOCK_TOTAL : dto.getStockTotal());
            stock.setSoldOut(Boolean.FALSE);
            dishStockService.apply(storeId, stock);
        }
        log.info("菜品已保存: storeId={}, dishId={}, isNew={}, specGroups={}, addons={}",
                storeId, dishId, isNew,
                dto.getSpecGroups() == null ? 0 : dto.getSpecGroups().size(),
                dto.getAddons() == null ? 0 : dto.getAddons().size());
        return dishId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long storeId, Long dishId) {
        require(storeId, dishId);
        List<Long> specIds = dishSpecMapper.selectList(new LambdaQueryWrapper<DishSpec>()
                        .eq(DishSpec::getDishId, dishId))
                .stream().map(DishSpec::getId).collect(Collectors.toList());
        if (!specIds.isEmpty()) {
            dishSpecOptionMapper.delete(new LambdaQueryWrapper<DishSpecOption>()
                    .in(DishSpecOption::getSpecId, specIds));
        }
        dishSpecMapper.delete(new LambdaQueryWrapper<DishSpec>().eq(DishSpec::getDishId, dishId));
        dishAddonMapper.delete(new LambdaQueryWrapper<DishAddon>().eq(DishAddon::getDishId, dishId));
        dishImageMapper.delete(new LambdaQueryWrapper<DishImage>().eq(DishImage::getDishId, dishId));
        dishTagRelMapper.delete(new LambdaQueryWrapper<DishTagRel>().eq(DishTagRel::getDishId, dishId));
        dishStockMapper.delete(new LambdaQueryWrapper<DishStock>().eq(DishStock::getDishId, dishId));
        dishMapper.deleteById(dishId);
    }

    @Override
    public void updateStatus(Long storeId, Long dishId, Integer status) {
        require(storeId, dishId);
        if (!Objects.equals(status, DishConstants.STATUS_OFF_SHELF)
                && !Objects.equals(status, DishConstants.STATUS_ON_SHELF)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "状态只允许 0 或 1");
        }
        Dish patch = new Dish();
        patch.setId(dishId);
        patch.setStatus(status);
        dishMapper.updateById(patch);
        log.info("菜品上下架: storeId={}, dishId={}, status={}", storeId, dishId, status);
    }

    @Override
    public void updateSort(Long storeId, Long dishId, Integer sort) {
        require(storeId, dishId);
        Dish patch = new Dish();
        patch.setId(dishId);
        patch.setSort(sort == null ? 0 : sort);
        dishMapper.updateById(patch);
    }

    private void replaceChildren(Long dishId, DishSaveDTO dto) {
        // null 表示本次不改动该子表，空数组才是显式清空
        if (dto.getSpecGroups() != null) {
            List<Long> oldSpecIds = dishSpecMapper.selectList(new LambdaQueryWrapper<DishSpec>()
                            .eq(DishSpec::getDishId, dishId))
                    .stream().map(DishSpec::getId).collect(Collectors.toList());
            if (!oldSpecIds.isEmpty()) {
                dishSpecOptionMapper.delete(new LambdaQueryWrapper<DishSpecOption>()
                        .in(DishSpecOption::getSpecId, oldSpecIds));
            }
            dishSpecMapper.delete(new LambdaQueryWrapper<DishSpec>().eq(DishSpec::getDishId, dishId));
            saveSpecGroups(dishId, dto);
        }
        if (dto.getAddons() != null) {
            dishAddonMapper.delete(new LambdaQueryWrapper<DishAddon>().eq(DishAddon::getDishId, dishId));
            saveAddons(dishId, dto);
        }
        if (dto.getImages() != null) {
            dishImageMapper.delete(new LambdaQueryWrapper<DishImage>().eq(DishImage::getDishId, dishId));
            saveImages(dishId, dto);
        }
    }

    private void saveSpecGroups(Long dishId, DishSaveDTO dto) {
        if (dto.getSpecGroups() == null) {
            return;
        }
        int groupSort = 0;
        for (DishSaveDTO.SpecGroupDTO group : dto.getSpecGroups()) {
            DishSpec spec = new DishSpec();
            spec.setDishId(dishId);
            spec.setSpecName(group.getSpecName());
            spec.setIsRequired(Boolean.FALSE.equals(group.getRequired())
                    ? DishConstants.NO : DishConstants.YES);
            spec.setSort(group.getSort() == null ? groupSort : group.getSort());
            dishSpecMapper.insert(spec);
            int optionSort = 0;
            if (group.getOptions() != null) {
                for (DishSaveDTO.SpecOptionDTO option : group.getOptions()) {
                    DishSpecOption record = new DishSpecOption();
                    record.setSpecId(spec.getId());
                    record.setOptionName(option.getOptionName());
                    record.setPriceDelta(option.getPriceDelta() == null
                            ? BigDecimal.ZERO : option.getPriceDelta());
                    record.setSort(option.getSort() == null ? optionSort : option.getSort());
                    dishSpecOptionMapper.insert(record);
                    optionSort++;
                }
            }
            groupSort++;
        }
    }

    /**
     * 会员价：未提交时保留原值（与划线价同口径），提交了就必须大于 0 且不高于售价。
     * 会员价只影响顾客端计价基数，规格与加料加价仍按售价口径叠加。
     */
    private BigDecimal resolveMemberPrice(DishSaveDTO dto, Dish current, BigDecimal price) {
        BigDecimal memberPrice = dto.getMemberPrice() == null
                ? (current == null ? null : current.getMemberPrice()) : dto.getMemberPrice();
        if (memberPrice == null) {
            return null;
        }
        if (memberPrice.signum() <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "会员价必须大于 0");
        }
        if (price != null && memberPrice.compareTo(price) > 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "会员价不能高于售价");
        }
        return memberPrice;
    }

    private void saveAddons(Long dishId, DishSaveDTO dto) {
        if (dto.getAddons() == null) {
            return;
        }
        for (DishSaveDTO.AddonDTO addon : dto.getAddons()) {
            DishAddon record = new DishAddon();
            record.setDishId(dishId);
            record.setAddonName(addon.getAddonName());
            record.setPriceDelta(addon.getPriceDelta() == null ? BigDecimal.ZERO : addon.getPriceDelta());
            dishAddonMapper.insert(record);
        }
    }

    private void saveImages(Long dishId, DishSaveDTO dto) {
        if (dto.getImages() == null) {
            return;
        }
        int sort = 0;
        for (DishSaveDTO.ImageDTO image : dto.getImages()) {
            if (!FileStorageService.isAllowedObjectKey(image.getObjectKey())) {
                throw new BizException(ErrorCode.FILE_OBJECT_KEY_INVALID);
            }
            DishImage record = new DishImage();
            record.setDishId(dishId);
            record.setObjectKey(image.getObjectKey());
            record.setImageType(StringUtils.hasText(image.getImageType())
                    ? image.getImageType() : "LIST");
            record.setSort(image.getSort() == null ? sort : image.getSort());
            dishImageMapper.insert(record);
            sort++;
        }
    }

    private void syncTags(Long dishId, List<Long> targetIds) {
        Set<Long> targets = targetIds == null
                ? Collections.emptySet() : new LinkedHashSet<>(targetIds);
        if (!targets.isEmpty() && dishTagMapper.selectBatchIds(targets).size() != targets.size()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "存在不合法的标签 ID");
        }
        List<Long> currentIds = dishTagRelMapper.selectList(new LambdaQueryWrapper<DishTagRel>()
                        .eq(DishTagRel::getDishId, dishId))
                .stream().map(DishTagRel::getTagId).collect(Collectors.toList());
        for (Long tagId : targets) {
            if (!currentIds.contains(tagId) && dishTagRelMapper.revive(dishId, tagId) == 0) {
                DishTagRel rel = new DishTagRel();
                rel.setDishId(dishId);
                rel.setTagId(tagId);
                dishTagRelMapper.insert(rel);
            }
        }
        List<Long> toRemove = currentIds.stream()
                .filter(tagId -> !targets.contains(tagId)).distinct().collect(Collectors.toList());
        if (!toRemove.isEmpty()) {
            dishTagRelMapper.delete(new LambdaQueryWrapper<DishTagRel>()
                    .eq(DishTagRel::getDishId, dishId)
                    .in(DishTagRel::getTagId, toRemove));
        }
    }

    private Dish require(Long storeId, Long dishId) {
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null || !Objects.equals(dish.getStoreId(), storeId)) {
            throw new BizException(ErrorCode.DISH_NOT_FOUND);
        }
        return dish;
    }

    private static String valueOrFallback(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
