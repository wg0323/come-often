package com.comeoften.module.review.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.review.dto.ReviewCreateDTO;
import com.comeoften.module.review.service.ReviewService;
import com.comeoften.module.review.vo.DishRatingVO;
import com.comeoften.module.review.vo.ReviewVO;
import com.comeoften.module.store.service.StoreService;
import com.comeoften.security.LoginUserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 顾客端评价。会员 ID 取登录态、门店取当前唯一门店，重复评价与订单状态由服务端判定。
 */
@Tag(name = "顾客端-评价", description = "提交评价、菜品评价分页与评分聚合")
@RestController
@RequestMapping("/api/app/review")
@RequiredArgsConstructor
public class ReviewAppController {

    private final ReviewService reviewService;

    private final StoreService storeService;

    @Operation(summary = "提交评价", description = "一单一评，订单需已完成；dishId 可空且必须属于该订单；"
            + "评价成功按 comeoften.points.review-points 发放积分")
    @ApiResponse(responseCode = "200", description = "返回评价详情")
    @ApiResponse(responseCode = "200", description = "重复评价 30051 / 订单未完成 30052 / 菜品不匹配 30053 / 图片不合法 30054")
    @PostMapping
    public Result<ReviewVO> create(@Valid @RequestBody ReviewCreateDTO dto) {
        return Result.ok(reviewService.create(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), dto));
    }

    @Operation(summary = "菜品评价分页", description = "按下评价时间倒序，包含脱敏昵称与评价图 objectKey")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @GetMapping("/dish/{dishId}")
    public Result<PageResult<ReviewVO>> pageByDish(
            @Parameter(description = "菜品 ID", required = true) @PathVariable Long dishId,
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数，最大 50") @RequestParam(defaultValue = "10") Long size) {
        return Result.ok(reviewService.pageByDish(dishId, current, size));
    }

    @Operation(summary = "菜品评分聚合", description = "平均分保留一位小数；无评价时 0.0 / 0 条")
    @ApiResponse(responseCode = "200", description = "返回平均分与评价条数")
    @GetMapping("/dish/{dishId}/rating")
    public Result<DishRatingVO> rating(
            @Parameter(description = "菜品 ID", required = true) @PathVariable Long dishId) {
        return Result.ok(reviewService.ratingOf(dishId));
    }
}
