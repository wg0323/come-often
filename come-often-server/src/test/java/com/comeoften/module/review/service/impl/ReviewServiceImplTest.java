package com.comeoften.module.review.service.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
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
import com.comeoften.module.review.constant.ReviewStatus;
import com.comeoften.module.review.dto.ReviewCreateDTO;
import com.comeoften.module.review.entity.Review;
import com.comeoften.module.review.entity.ReviewImage;
import com.comeoften.module.review.mapper.ReviewImageMapper;
import com.comeoften.module.review.mapper.ReviewMapper;
import com.comeoften.module.review.vo.ReviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 评价提交的四条拒绝分支与一条成功分支：一单一评、订单状态、菜品归属、图片前缀，
 * 以及「积分幂等键必须等于评价 ID」这条口径。
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    private static final Long MEMBER_ID = 7001L;

    private static final Long STORE_ID = 1L;

    private static final Long ORDER_ID = 8001L;

    private static final Long DISH_ID = 1002L;

    private static final String ORDER_NO = "CO20261109120000ABCDEF";

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private ReviewImageMapper reviewImageMapper;

    @Mock
    private TradeOrderMapper tradeOrderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private DishMapper dishMapper;

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private PointsAccountService pointsAccountService;

    @Mock
    private FileStorageService fileStorageService;

    @Spy
    private PointsProperties pointsProperties = new PointsProperties();

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private TradeOrder order;

    private ReviewCreateDTO dto;

    @BeforeEach
    void setUp() {
        order = new TradeOrder();
        order.setId(ORDER_ID);
        order.setOrderNo(ORDER_NO);
        order.setMemberId(MEMBER_ID);
        order.setStoreId(STORE_ID);
        order.setStatus(OrderStatus.COMPLETED.name());
        dto = new ReviewCreateDTO();
        dto.setOrderNo(ORDER_NO);
        dto.setRating(5);
        dto.setContent("分量足");
        // 审核分支不走订单详情查询，lenient 避免严格模式下把公共 stub 判成多余
        lenient().when(tradeOrderMapper.selectOne(any())).thenReturn(order);
    }

    @Test
    void should_reject_duplicate_review_when_order_already_reviewed() {
        when(orderItemMapper.selectList(any())).thenReturn(Collections.singletonList(item(DISH_ID)));
        when(reviewMapper.selectCount(any())).thenReturn(1L);

        BizException ex = catchThrowableOfType(() -> reviewService.create(MEMBER_ID, STORE_ID, dto),
                BizException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_DUPLICATE);
        verify(reviewMapper, never()).insert(any(Review.class));
        verify(pointsAccountService, never()).earn(any(), anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    void should_reject_review_when_order_is_not_completed() {
        order.setStatus(OrderStatus.PAID.name());

        BizException ex = catchThrowableOfType(() -> reviewService.create(MEMBER_ID, STORE_ID, dto),
                BizException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_ORDER_NOT_COMPLETED);
        verify(reviewMapper, never()).insert(any(Review.class));
    }

    @Test
    void should_reject_review_when_dish_is_not_in_the_order() {
        dto.setDishId(9999L);
        when(orderItemMapper.selectList(any())).thenReturn(Collections.singletonList(item(DISH_ID)));

        BizException ex = catchThrowableOfType(() -> reviewService.create(MEMBER_ID, STORE_ID, dto),
                BizException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_DISH_INVALID);
        verify(reviewMapper, never()).insert(any(Review.class));
    }

    @Test
    void should_reject_review_when_image_key_is_not_a_review_key() {
        dto.setImageObjectKeys(Collections.singletonList("dish/1002_list.webp"));
        when(orderItemMapper.selectList(any())).thenReturn(Collections.singletonList(item(DISH_ID)));

        BizException ex = catchThrowableOfType(() -> reviewService.create(MEMBER_ID, STORE_ID, dto),
                BizException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_IMAGE_INVALID);
        verify(reviewMapper, never()).insert(any(Review.class));
    }

    @Test
    void should_reject_review_when_image_object_is_missing_in_storage() {
        dto.setImageObjectKeys(Collections.singletonList("review/not-uploaded.webp"));
        when(orderItemMapper.selectList(any())).thenReturn(Collections.singletonList(item(DISH_ID)));
        when(fileStorageService.exists("review/not-uploaded.webp")).thenReturn(false);

        BizException ex = catchThrowableOfType(() -> reviewService.create(MEMBER_ID, STORE_ID, dto),
                BizException.class);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_IMAGE_INVALID);
        verify(reviewMapper, never()).insert(any(Review.class));
    }

    @Test
    void should_grant_points_with_review_id_when_review_created() {
        dto.setDishId(DISH_ID);
        dto.setImageObjectKeys(Arrays.asList("review/a.webp", "review/b.webp", "review/c.webp"));
        when(orderItemMapper.selectList(any())).thenReturn(Collections.singletonList(item(DISH_ID)));
        when(reviewMapper.selectCount(any())).thenReturn(0L);
        when(fileStorageService.exists(anyString())).thenReturn(true);
        when(reviewMapper.insert(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(555L);
            return 1;
        });
        Dish dish = new Dish();
        dish.setId(DISH_ID);
        dish.setDishName("九转大肠");
        when(dishMapper.selectById(DISH_ID)).thenReturn(dish);
        Member member = new Member();
        member.setId(MEMBER_ID);
        member.setNickname("老张");
        when(memberMapper.selectById(MEMBER_ID)).thenReturn(member);
        when(pointsAccountService.earn(any(), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(10);

        ReviewVO vo = reviewService.create(MEMBER_ID, STORE_ID, dto);

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).insert(reviewCaptor.capture());
        Review saved = reviewCaptor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(saved.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(saved.getDishId()).isEqualTo(DISH_ID);
        assertThat(saved.getIsWithImage()).isEqualTo(1);
        verify(reviewImageMapper, times(3)).insert(any(ReviewImage.class));
        // 先取值再传 eq()：直接在 matcher 里调 @Spy 的方法会被 Mockito 当成带 matcher 的 mock 调用
        int configuredReviewPoints = pointsProperties.getReviewPoints();
        verify(pointsAccountService).earn(eq(MEMBER_ID), eq(PointsBizType.REVIEW), eq("555"),
                eq(configuredReviewPoints), anyString());
        assertThat(vo.getWithImage()).isTrue();
        assertThat(vo.getImageObjectKeys()).hasSize(3);
        assertThat(vo.getNickname()).isEqualTo("老**");
        assertThat(vo.getDishName()).isEqualTo("九转大肠");
    }

    @Test
    void should_keep_distinct_image_order_when_same_key_submitted_twice() {
        dto.setImageObjectKeys(Arrays.asList("review/a.webp", "review/a.webp", "review/b.webp"));
        when(orderItemMapper.selectList(any())).thenReturn(Collections.singletonList(item(DISH_ID)));
        when(reviewMapper.selectCount(any())).thenReturn(0L);
        when(fileStorageService.exists(anyString())).thenReturn(true);
        when(reviewMapper.insert(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(556L);
            return 1;
        });
        when(pointsAccountService.earn(any(), anyString(), anyString(), anyInt(), anyString()))
                .thenReturn(10);

        ReviewVO vo = reviewService.create(MEMBER_ID, STORE_ID, dto);

        verify(reviewImageMapper, times(2)).insert(any(ReviewImage.class));
        assertThat(vo.getImageObjectKeys()).containsExactly("review/a.webp", "review/b.webp");
    }

    @Test
  void should_reject_audit_when_target_status_invalid() {
    BizException ex = catchThrowableOfType(
        () -> reviewService.audit(STORE_ID, 555L, "UNKNOWN", 9L, ""), BizException.class);

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_STATUS_INVALID);
    verify(reviewMapper, never()).audit(any(), any(), anyString(), any());
  }

  @Test
  void should_reject_audit_when_review_not_found() {
    when(reviewMapper.selectById(555L)).thenReturn(null);

    BizException ex = catchThrowableOfType(
        () -> reviewService.audit(STORE_ID, 555L, "APPROVED", 9L, ""), BizException.class);

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_FOUND);
  }

  @Test
  void should_reject_audit_when_status_unchanged() {
    when(reviewMapper.selectById(555L)).thenReturn(review(ReviewStatus.APPROVED));

    BizException ex = catchThrowableOfType(
        () -> reviewService.audit(STORE_ID, 555L, "APPROVED", 9L, ""), BizException.class);

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_ALREADY_AUDITED);
    verify(reviewMapper, never()).audit(any(), any(), anyString(), any());
  }

  @Test
  void should_audit_review_when_status_changes() {
    when(reviewMapper.selectById(555L)).thenReturn(review(ReviewStatus.PENDING));
    when(reviewMapper.audit(any(), any(), anyString(), any())).thenReturn(1);
    when(reviewImageMapper.selectList(any())).thenReturn(Collections.emptyList());
    when(memberMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());
    when(tradeOrderMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());

    ReviewVO vo = reviewService.audit(STORE_ID, 555L, "APPROVED", 9L, "");

    assertThat(vo.getStatus()).isEqualTo("APPROVED");
    assertThat(vo.getStatusText()).isEqualTo("已通过");
    assertThat(vo.getAuditUserId()).isEqualTo(9L);
  }

  private static Review review(ReviewStatus status) {
    Review review = new Review();
    review.setId(555L);
    review.setStoreId(STORE_ID);
    review.setMemberId(MEMBER_ID);
    review.setOrderId(ORDER_ID);
    review.setRating(5);
    review.setContent("分量足");
    review.setStatus(status.name());
    return review;
  }

  private static OrderItem item(Long dishId) {
        OrderItem item = new OrderItem();
        item.setOrderId(ORDER_ID);
        item.setDishId(dishId);
        item.setDishName("九转大肠");
        return item;
    }
}
