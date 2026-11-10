package com.comeoften.module.review.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.PageResult;
import com.comeoften.config.PointsProperties;
import com.comeoften.integration.storage.FileStorageService;
import com.comeoften.module.auth.entity.Member;
import com.comeoften.module.auth.mapper.MemberMapper;
import com.comeoften.module.dish.entity.Dish;
import com.comeoften.module.dish.mapper.DishMapper;
import com.comeoften.module.order.constant.OrderStatus;
import com.comeoften.module.order.entity.OrderItem;
import com.comeoften.module.order.entity.TradeOrder;
import com.comeoften.module.order.mapper.OrderItemMapper;
import com.comeoften.module.order.mapper.TradeOrderMapper;
import com.comeoften.module.points.constant.PointsBizType;
import com.comeoften.module.points.service.PointsAccountService;
import com.comeoften.module.points.util.PointsBizKeys;
import com.comeoften.module.review.constant.ReviewStatus;
import com.comeoften.module.review.converter.ReviewConverter;
import com.comeoften.module.review.dto.ReviewCreateDTO;
import com.comeoften.module.review.entity.Review;
import com.comeoften.module.review.entity.ReviewImage;
import com.comeoften.module.review.mapper.ReviewImageMapper;
import com.comeoften.module.review.mapper.ReviewMapper;
import com.comeoften.module.review.mapper.ReviewRatingAggregate;
import com.comeoften.module.review.service.ReviewService;
import com.comeoften.module.review.util.ReviewRatingCalculator;
import com.comeoften.module.review.vo.DishRatingVO;
import com.comeoften.module.review.vo.ReviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评价实现。
 *
 * <p>一单一评由 uk_order_member 兜底，重复提交直接返回 30051；评价图只接受 review/ 前缀的
 * objectKey，不接受完整 URL。积分复用 PointsAccountService.earn，幂等键是 reviewId，
 * 因此同一评价重复补发不会多给积分。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final String REVIEW_IMAGE_PREFIX = "review/";

    private static final int MAX_IMAGES = 3;

    private static final long DEFAULT_PAGE_SIZE = 10L;

    private static final long MAX_PAGE_SIZE = 50L;

    private static final String REVIEW_REMARK = "评价奖励";

    private final ReviewMapper reviewMapper;

    private final ReviewImageMapper reviewImageMapper;

    private final TradeOrderMapper tradeOrderMapper;

    private final OrderItemMapper orderItemMapper;

    private final DishMapper dishMapper;

    private final MemberMapper memberMapper;

    private final PointsAccountService pointsAccountService;

    private final PointsProperties pointsProperties;

    private final FileStorageService fileStorageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewVO create(Long memberId, Long storeId, ReviewCreateDTO dto) {
        TradeOrder order = requireCompletedOrder(memberId, storeId, dto.getOrderNo());
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        Long dishId = resolveDishId(dto.getDishId(), items);
        List<String> imageKeys = normalizeImages(dto.getImageObjectKeys());
        if (reviewMapper.selectCount(new LambdaQueryWrapper<Review>()
                .eq(Review::getOrderId, order.getId())
                .eq(Review::getMemberId, memberId)) > 0) {
            throw new BizException(ErrorCode.REVIEW_DUPLICATE);
        }

        Review review = new Review();
        review.setStoreId(storeId);
        review.setOrderId(order.getId());
        review.setMemberId(memberId);
        review.setDishId(dishId);
        review.setRating(dto.getRating());
        review.setContent(StringUtils.hasText(dto.getContent()) ? dto.getContent().trim() : "");
        review.setIsWithImage(imageKeys.isEmpty() ? 0 : 1);
        // 先发后审：新建评价立即通过（与 DB 默认值一致，历史评价无需刷数据），后台仍可改为拒绝 / 待复核
        review.setStatus(ReviewStatus.APPROVED.name());
        try {
            reviewMapper.insert(review);
        } catch (DuplicateKeyException e) {
            // 并发重复提交时由唯一索引兜底，语义与前置查重一致
            throw new BizException(ErrorCode.REVIEW_DUPLICATE);
        }
        List<ReviewImage> images = saveImages(review.getId(), imageKeys);
        int points = pointsAccountService.earn(memberId, PointsBizType.REVIEW,
                PointsBizKeys.review(review.getId()), pointsProperties.getReviewPoints(), REVIEW_REMARK);
        log.info("评价已提交: reviewId={}, orderNo={}, memberId={}, dishId={}, images={}, points={}",
                review.getId(), dto.getOrderNo(), memberId, dishId, images.size(), points);
        return ReviewConverter.toVO(review, images, dishNameOf(dishId), nicknameOf(memberId));
    }

    @Override
    public PageResult<ReviewVO> pageByDish(Long dishId, Long current, Long size) {
        if (dishId == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "菜品 ID 不能为空");
        }
        long pageNo = normalizePageNo(current);
        long pageSize = normalizePageSize(size);
        IPage<Review> page = reviewMapper.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getDishId, dishId)
                        // 只有通过的评价对顾客可见；待复核与已拒绝都不出现在菜品评价区
                        .eq(Review::getStatus, ReviewStatus.APPROVED.name())
                        .orderByDesc(Review::getCreateTime)
                        .orderByDesc(Review::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), toVOs(page.getRecords()));
    }

    @Override
    public PageResult<ReviewVO> pageForAdmin(Long storeId, String status, Long dishId,
                                              Long current, Long size) {
        ReviewStatus target = StringUtils.hasText(status) ? ReviewStatus.of(status) : null;
        if (StringUtils.hasText(status) && target == null) {
            throw new BizException(ErrorCode.REVIEW_STATUS_INVALID);
        }
        IPage<Review> page = reviewMapper.selectPage(new Page<>(normalizePageNo(current),
                        normalizePageSize(size)),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getStoreId, storeId)
                        .eq(target != null, Review::getStatus, target == null ? null : target.name())
                        .eq(dishId != null, Review::getDishId, dishId)
                        .orderByDesc(Review::getCreateTime)
                        .orderByDesc(Review::getId));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), toVOs(page.getRecords()));
    }

    @Override
    public ReviewVO detailForAdmin(Long storeId, Long reviewId) {
        Review review = requireByStore(storeId, reviewId);
        return toVOs(Collections.singletonList(review)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewVO audit(Long storeId, Long reviewId, String targetStatus,
                           Long operatorId, String remark) {
        ReviewStatus target = ReviewStatus.of(targetStatus);
        if (target == null) {
            throw new BizException(ErrorCode.REVIEW_STATUS_INVALID);
        }
        Review review = requireByStore(storeId, reviewId);
        String from = review.getStatus();
        if (target.name().equals(from)) {
            throw new BizException(ErrorCode.REVIEW_ALREADY_AUDITED);
        }
        // 带旧状态条件的 CAS：并发两次审核同一目标状态时只有一个成功
        if (reviewMapper.audit(reviewId, storeId, target.name(), operatorId) == 0) {
            throw new BizException(ErrorCode.REVIEW_ALREADY_AUDITED);
        }
        review.setStatus(target.name());
        review.setAuditUserId(operatorId);
        review.setAuditTime(LocalDateTime.now());
        log.info("评价审核完成: reviewId={}, from={}, to={}, operatorId={}, remark={}",
                reviewId, from, target.name(), operatorId, remark);
        return toVOs(Collections.singletonList(review)).get(0);
    }

    private static long normalizePageNo(Long current) {
        return current == null || current < 1 ? 1L : current;
    }

    private static long normalizePageSize(Long size) {
        return size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
    }

    private Review requireByStore(Long storeId, Long reviewId) {
        if (reviewId == null) {
            throw new BizException(ErrorCode.REVIEW_NOT_FOUND);
        }
        Review review = reviewMapper.selectById(reviewId);
        if (review == null || !Objects.equals(storeId, review.getStoreId())) {
            throw new BizException(ErrorCode.REVIEW_NOT_FOUND);
        }
        return review;
    }

    /**
     * 顾客端评价列表、后台分页与审核结果共用同一份装配：图片、菜名、昵称、订单号批量查回。
     */
    private List<ReviewVO> toVOs(List<Review> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> reviewIds = records.stream().map(Review::getId).collect(Collectors.toList());
        Map<Long, List<ReviewImage>> imageMap = reviewImageMapper.selectList(
                        new LambdaQueryWrapper<ReviewImage>()
                                .in(ReviewImage::getReviewId, reviewIds)
                                .orderByAsc(ReviewImage::getSort))
                .stream().collect(Collectors.groupingBy(ReviewImage::getReviewId));
        Map<Long, String> dishNames = loadDishNames(records);
        Map<Long, String> nicknames = loadNicknames(records);
        Map<Long, String> orderNos = loadOrderNos(records);
        return records.stream()
                .map(review -> {
                    ReviewVO vo = ReviewConverter.toVO(review,
                            imageMap.getOrDefault(review.getId(), Collections.emptyList()),
                            dishNames.get(review.getDishId()), nicknames.get(review.getMemberId()));
                    vo.setOrderNo(orderNos.get(review.getOrderId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public DishRatingVO ratingOf(Long dishId) {
        if (dishId == null) {
            return DishRatingVO.of(ReviewRatingCalculator.average(Collections.emptyList()), 0L);
        }
        ReviewRatingAggregate aggregate = reviewMapper.ratingOf(dishId);
        if (aggregate == null || aggregate.getReviewCount() == null || aggregate.getReviewCount() == 0L) {
            return DishRatingVO.of(ReviewRatingCalculator.average(Collections.emptyList()), 0L);
        }
        return DishRatingVO.of(ReviewRatingCalculator.average(aggregate.getAverageRating()),
                aggregate.getReviewCount());
    }

    @Override
    public Set<Long> reviewedOrderIds(Long memberId, Collection<Long> orderIds) {
        if (memberId == null || orderIds == null || orderIds.isEmpty()) {
            return Collections.emptySet();
        }
        return reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                        .select(Review::getOrderId)
                        .eq(Review::getMemberId, memberId)
                        .in(Review::getOrderId, orderIds))
                .stream().map(Review::getOrderId).collect(Collectors.toSet());
    }

    // ------------------------------------------------------------------ 私有逻辑

    private TradeOrder requireCompletedOrder(Long memberId, Long storeId, String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "订单号不能为空");
        }
        TradeOrder order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!memberId.equals(order.getMemberId()) || !storeId.equals(order.getStoreId())) {
            throw new BizException(ErrorCode.ORDER_ACCESS_DENIED);
        }
        if (!OrderStatus.COMPLETED.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.REVIEW_ORDER_NOT_COMPLETED);
        }
        return order;
    }

    /**
     * dishId 可空；有值时必须出现在该订单的明细快照里，避免给没点过的菜刷评价。
     */
    private Long resolveDishId(Long dishId, List<OrderItem> items) {
        if (dishId == null) {
            return null;
        }
        boolean matched = items.stream()
                .anyMatch(item -> dishId.equals(item.getDishId()));
        if (!matched) {
            throw new BizException(ErrorCode.REVIEW_DISH_INVALID);
        }
        return dishId;
    }

    private List<String> normalizeImages(List<String> imageObjectKeys) {
        if (imageObjectKeys == null || imageObjectKeys.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String objectKey : imageObjectKeys) {
            if (!StringUtils.hasText(objectKey)) {
                continue;
            }
            String trimmed = objectKey.trim();
            // 只收 review/ 前缀：会员不能把菜品图、轮播图挂成自己的评价图
            if (!trimmed.startsWith(REVIEW_IMAGE_PREFIX) || !FileStorageService.isAllowedObjectKey(trimmed)) {
                throw new BizException(ErrorCode.REVIEW_IMAGE_INVALID);
            }
            // 对象必须真实存在：评价没有编辑入口，挂一个不存在的 key 会永久留一张坏图
            if (!fileStorageService.exists(trimmed)) {
                throw new BizException(ErrorCode.REVIEW_IMAGE_INVALID);
            }
            unique.add(trimmed);
        }
        if (unique.size() > MAX_IMAGES) {
            throw new BizException(ErrorCode.REVIEW_IMAGE_INVALID);
        }
        return new ArrayList<>(unique);
    }

    private List<ReviewImage> saveImages(Long reviewId, List<String> objectKeys) {
        if (objectKeys.isEmpty()) {
            return Collections.emptyList();
        }
        List<ReviewImage> saved = new ArrayList<>(objectKeys.size());
        for (int index = 0; index < objectKeys.size(); index++) {
            ReviewImage image = new ReviewImage();
            image.setReviewId(reviewId);
            image.setObjectKey(objectKeys.get(index));
            image.setSort(index);
            reviewImageMapper.insert(image);
            saved.add(image);
        }
        return saved;
    }

    private String dishNameOf(Long dishId) {
        if (dishId == null) {
            return "";
        }
        Dish dish = dishMapper.selectById(dishId);
        return dish == null ? "" : dish.getDishName();
    }

    private String nicknameOf(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        return member == null ? null : member.getNickname();
    }

    private Map<Long, String> loadDishNames(List<Review> records) {
        List<Long> dishIds = records.stream().map(Review::getDishId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (dishIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return dishMapper.selectBatchIds(dishIds).stream()
                .collect(Collectors.toMap(Dish::getId, Dish::getDishName, (left, right) -> left));
    }

    private Map<Long, String> loadOrderNos(List<Review> records) {
        List<Long> orderIds = records.stream().map(Review::getOrderId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<TradeOrder> orders = tradeOrderMapper.selectBatchIds(orderIds);
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyMap();
        }
        return orders.stream()
                .collect(Collectors.toMap(TradeOrder::getId, TradeOrder::getOrderNo,
                        (left, right) -> left));
    }

    private Map<Long, String> loadNicknames(List<Review> records) {
        List<Long> memberIds = records.stream().map(Review::getMemberId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (memberIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return memberMapper.selectBatchIds(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, member -> member.getNickname() == null
                        ? "" : member.getNickname(), (left, right) -> left));
    }
}
