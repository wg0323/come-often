package com.comeoften.module.review.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 菜品评分聚合。平均分保留一位小数，无评价时为 0.0。
 */
@Data
@Schema(description = "菜品评分聚合")
public class DishRatingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "平均分，一位小数", example = "4.5")
    private BigDecimal averageRating;

    @Schema(description = "评价条数", example = "12")
    private Long reviewCount;

    public static DishRatingVO of(BigDecimal averageRating, Long reviewCount) {
        DishRatingVO vo = new DishRatingVO();
        vo.setAverageRating(averageRating);
        vo.setReviewCount(reviewCount == null ? 0L : reviewCount);
        return vo;
    }
}
