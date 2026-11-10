package com.comeoften.module.review.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.review.dto.ReviewAuditDTO;
import com.comeoften.module.review.service.ReviewService;
import com.comeoften.module.review.vo.ReviewVO;
import com.comeoften.security.LoginUserHolder;
import com.comeoften.security.RequirePerm;
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
 * PC 后台评价审核。权限点 review:audit（B1 预登记的菜单 112）。
 */
@Tag(name = "PC 后台-评价审核", description = "评价分页、详情与通过 / 拒绝 / 转待复核")
@RestController
@RequestMapping("/api/admin/review")
@RequiredArgsConstructor
public class ReviewAdminController {

    private final ReviewService reviewService;

    @Operation(summary = "评价分页", description = "按审核状态过滤；含菜品名、脱敏昵称与评价图")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @RequirePerm("review:audit")
    @GetMapping("/page")
    public Result<PageResult<ReviewVO>> page(
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "审核状态：PENDING/APPROVED/REJECTED，空表示全部")
            @RequestParam(required = false) String status,
            @Parameter(description = "菜品 ID") @RequestParam(required = false) Long dishId) {
        return Result.ok(reviewService.pageForAdmin(LoginUserHolder.getRequired().getStoreId(),
                status, dishId, current, size));
    }

    @Operation(summary = "评价详情", description = "含审核状态与审核人、审核时间")
    @ApiResponse(responseCode = "200", description = "返回评价详情")
    @RequirePerm("review:audit")
    @GetMapping("/{reviewId}")
    public Result<ReviewVO> detail(
            @Parameter(description = "评价 ID", required = true) @PathVariable Long reviewId) {
        return Result.ok(reviewService.detailForAdmin(LoginUserHolder.getRequired().getStoreId(), reviewId));
    }

    @Operation(summary = "审核评价", description = "重复提交同一状态返回 30087；拒绝后评价不再计入评分聚合")
    @ApiResponse(responseCode = "200", description = "返回审核后的评价")
    @RequirePerm("review:audit")
    @PostMapping("/{reviewId}/audit")
    public Result<ReviewVO> audit(
            @Parameter(description = "评价 ID", required = true) @PathVariable Long reviewId,
            @Valid @RequestBody ReviewAuditDTO dto) {
        return Result.ok(reviewService.audit(LoginUserHolder.getRequired().getStoreId(), reviewId,
                dto.getStatus(), LoginUserHolder.getRequired().getUserId(), dto.getRemark()));
    }
}
