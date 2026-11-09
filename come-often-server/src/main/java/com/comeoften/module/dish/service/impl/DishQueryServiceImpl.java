package com.comeoften.module.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.dish.converter.DishConverter;
import com.comeoften.module.dish.converter.DishListAssembler;
import com.comeoften.module.dish.dto.DishQueryDTO;
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
import com.comeoften.module.dish.util.DishConstants;
import com.comeoften.module.dish.vo.DishDetailVO;
import com.comeoften.module.dish.vo.DishListVO;
import com.comeoften.module.review.service.ReviewService;
import com.comeoften.module.review.vo.DishRatingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 顾客端菜品展示。下架菜品不出现在列表，直接访问详情返回「菜品已下架」。
 */
@Service
@RequiredArgsConstructor
public class DishQueryServiceImpl implements DishQueryService {

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

    private final ReviewService reviewService;

    @Override
    public List<DishListVO> list(DishQueryDTO query) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<Dish>()
                .eq(Dish::getStoreId, query.getStoreId())
                .eq(Dish::getStatus, DishConstants.STATUS_ON_SHELF);
        if (query.getCategoryId() != null) {
            wrapper.eq(Dish::getCategoryId, query.getCategoryId());
        }
        if (Boolean.TRUE.equals(query.getPreorder())) {
            wrapper.eq(Dish::getIsPreorder, DishConstants.YES);
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.likeRight(Dish::getDishName, query.getKeyword().trim());
        }
        if (Boolean.TRUE.equals(query.getSignature())) {
            List<Long> signatureIds = findSignatureDishIds();
            if (signatureIds.isEmpty()) {
                return Collections.emptyList();
            }
            wrapper.in(Dish::getId, signatureIds);
        }
        wrapper.orderByAsc(Dish::getSort).orderByAsc(Dish::getId);
        return dishListAssembler.assemble(query.getStoreId(), dishMapper.selectList(wrapper));
    }

    @Override
    public DishDetailVO detail(Long dishId, Long storeId) {
        Dish dish = requireDish(dishId, storeId);
        if (!DishConverter.isOnShelf(dish)) {
            throw new BizException(ErrorCode.DISH_OFF_SHELF);
        }
        return buildDetail(dish);
    }

    @Override
    public DishDetailVO detailForAdmin(Long storeId, Long dishId) {
        return buildDetail(requireDish(dishId, storeId));
    }

    private DishDetailVO buildDetail(Dish dish) {
        DishCategory category = dishCategoryMapper.selectById(dish.getCategoryId());
        String categoryName = category == null ? ""
                : DishConverter.displayCategoryName(category.getCategoryName());
        DishDetailVO vo = DishConverter.toDetailBase(dish, categoryName);
        vo.setImages(dishImageMapper.selectList(new LambdaQueryWrapper<DishImage>()
                        .eq(DishImage::getDishId, dish.getId())
                        .orderByAsc(DishImage::getSort))
                .stream().map(DishConverter::toImageVO).collect(Collectors.toList()));
        vo.setSpecGroups(buildSpecGroups(dish.getId()));
        vo.setAddons(dishAddonMapper.selectList(new LambdaQueryWrapper<DishAddon>()
                        .eq(DishAddon::getDishId, dish.getId()))
                .stream().map(DishConverter::toAddonVO).collect(Collectors.toList()));
        List<Long> tagIds = dishTagRelMapper.selectList(new LambdaQueryWrapper<DishTagRel>()
                        .eq(DishTagRel::getDishId, dish.getId()))
                .stream().map(DishTagRel::getTagId).distinct().collect(Collectors.toList());
        vo.setTagIds(tagIds);
        vo.setTags(tagIds.isEmpty() ? Collections.emptyList()
                : dishTagMapper.selectBatchIds(tagIds).stream()
                .map(DishTag::getTagName).collect(Collectors.toList()));
        DishStock stock = loadTodayStock(dish.getStoreId(), dish.getId());
        vo.setStockRemain(stock == null ? null : stock.getRemain());
        vo.setSoldOut(stock != null && stock.getRemain() <= 0);
        DishRatingVO rating = reviewService.ratingOf(dish.getId());
        vo.setRatingAverage(rating.getAverageRating());
        vo.setReviewCount(rating.getReviewCount());
        return vo;
    }

    private List<DishDetailVO.SpecGroupVO> buildSpecGroups(Long dishId) {
        List<DishSpec> specs = dishSpecMapper.selectList(new LambdaQueryWrapper<DishSpec>()
                .eq(DishSpec::getDishId, dishId)
                .orderByAsc(DishSpec::getSort));
        if (specs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> specIds = specs.stream().map(DishSpec::getId).collect(Collectors.toList());
        Map<Long, List<DishSpecOption>> optionMap = dishSpecOptionMapper.selectList(
                        new LambdaQueryWrapper<DishSpecOption>()
                                .in(DishSpecOption::getSpecId, specIds)
                                .orderByAsc(DishSpecOption::getSort))
                .stream().collect(Collectors.groupingBy(DishSpecOption::getSpecId));
        return specs.stream()
                .map(spec -> DishConverter.toSpecGroupVO(spec,
                        optionMap.getOrDefault(spec.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    private List<Long> findSignatureDishIds() {
        DishTag tag = dishTagMapper.selectOne(new LambdaQueryWrapper<DishTag>()
                .eq(DishTag::getTagCode, DishConstants.SIGNATURE_TAG_CODE));
        if (tag == null) {
            return Collections.emptyList();
        }
        return dishTagRelMapper.selectList(new LambdaQueryWrapper<DishTagRel>()
                        .eq(DishTagRel::getTagId, tag.getId()))
                .stream().map(DishTagRel::getDishId).distinct().collect(Collectors.toList());
    }

    private DishStock loadTodayStock(Long storeId, Long dishId) {
        return dishStockMapper.selectOne(new LambdaQueryWrapper<DishStock>()
                .eq(DishStock::getStoreId, storeId)
                .eq(DishStock::getDishId, dishId)
                .eq(DishStock::getStockDate, LocalDate.now()));
    }

    private Dish requireDish(Long dishId, Long storeId) {
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null || (storeId != null && !storeId.equals(dish.getStoreId()))) {
            throw new BizException(ErrorCode.DISH_NOT_FOUND);
        }
        return dish;
    }
}
