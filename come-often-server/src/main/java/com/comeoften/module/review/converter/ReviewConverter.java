package com.comeoften.module.review.converter;

import com.comeoften.module.review.entity.Review;
import com.comeoften.module.review.entity.ReviewImage;
import com.comeoften.module.review.vo.ReviewVO;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评价对象转换。字段名不同（isWithImage → withImage）且图片要摊平成 objectKey 列表，
 * 属于非等价映射，统一收在这里。
 */
public final class ReviewConverter {

    private static final String ANONYMOUS_NICKNAME = "匿名会员";

    private static final String NICKNAME_MASK = "**";

    private ReviewConverter() {
    }

    public static ReviewVO toVO(Review review, List<ReviewImage> images, String dishName, String nickname) {
        ReviewVO vo = new ReviewVO();
        vo.setId(review.getId());
        vo.setDishId(review.getDishId());
        vo.setDishName(dishName);
        vo.setNickname(maskNickname(nickname));
        vo.setRating(review.getRating());
        vo.setContent(review.getContent());
        vo.setWithImage(Integer.valueOf(1).equals(review.getIsWithImage()));
        vo.setImageObjectKeys(images == null ? Collections.emptyList()
                : images.stream().map(ReviewImage::getObjectKey).collect(Collectors.toList()));
        vo.setCreateTime(review.getCreateTime());
        return vo;
    }

    /**
     * 昵称只保留首字，避免评价区暴露会员身份。
     * 按码点截取：emoji 昵称是代理对，按 char 截会切出半个字符。
     */
    public static String maskNickname(String nickname) {
        if (!StringUtils.hasText(nickname)) {
            return ANONYMOUS_NICKNAME;
        }
        String trimmed = nickname.trim();
        int end = trimmed.offsetByCodePoints(0, 1);
        return trimmed.substring(0, end) + NICKNAME_MASK;
    }
}
