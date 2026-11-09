package com.comeoften.module.dish.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.dish.dto.DishSaveDTO;
import com.comeoften.module.dish.service.DishService;
import com.comeoften.module.dish.vo.DishDetailVO;
import com.comeoften.module.dish.vo.DishListVO;
import com.comeoften.security.LoginUserHolder;
import com.comeoften.security.RequirePerm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * PC 后台菜品 CRUD。门店 ID 取自当前登录员工，所有接口需要 dish:manage 权限点。
 */
@Tag(name = "PC 后台-菜品管理", description = "菜品 CRUD、批量上下架与排序；规格、加料、图片整体覆盖式保存")
@RestController
@RequestMapping("/api/admin/dish")
@RequiredArgsConstructor
public class DishAdminController {

    private final DishService dishService;

    @Operation(summary = "菜品分页列表", description = "可按菜名、分类、状态过滤，含售罄状态")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @RequirePerm("dish:manage")
    @GetMapping("/page")
    public Result<PageResult<DishListVO>> page(
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "菜名关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类 ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "状态：0-下架，1-上架") @RequestParam(required = false) Integer status) {
        return Result.ok(dishService.page(LoginUserHolder.getRequired().getStoreId(),
                current, size, keyword, categoryId, status));
    }

    @Operation(summary = "菜品详情回显", description = "允许查看下架菜品，供编辑弹窗使用")
    @ApiResponse(responseCode = "200", description = "返回含规格、加料、图片的完整详情")
    @RequirePerm("dish:manage")
    @GetMapping("/{dishId}")
    public Result<DishDetailVO> detail(
            @Parameter(description = "菜品 ID", required = true) @PathVariable Long dishId) {
        return Result.ok(dishService.detailForAdmin(LoginUserHolder.getRequired().getStoreId(), dishId));
    }

    @Operation(summary = "新增菜品", description = "可一次提交两规格一加料；stockTotal 初始化当日库存")
    @ApiResponse(responseCode = "200", description = "返回新菜品 ID")
    @RequirePerm("dish:manage")
    @PostMapping
    public Result<String> create(@Valid @RequestBody DishSaveDTO dto) {
        dto.setId(null);
        return Result.ok(String.valueOf(dishService.save(LoginUserHolder.getRequired().getStoreId(), dto)));
    }

    @Operation(summary = "编辑菜品", description = "整体覆盖规格、加料、图片与标签")
    @ApiResponse(responseCode = "200", description = "返回菜品 ID")
    @RequirePerm("dish:manage")
    @PutMapping("/{dishId}")
    public Result<String> update(
            @Parameter(description = "菜品 ID", required = true) @PathVariable Long dishId,
            @Valid @RequestBody DishSaveDTO dto) {
        dto.setId(dishId);
        return Result.ok(String.valueOf(dishService.save(LoginUserHolder.getRequired().getStoreId(), dto)));
    }

    @Operation(summary = "删除菜品", description = "逻辑删除菜品及其规格、加料、图片与标签关联")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @RequirePerm("dish:manage")
    @DeleteMapping("/{dishId}")
    public Result<Void> remove(
            @Parameter(description = "菜品 ID", required = true) @PathVariable Long dishId) {
        dishService.remove(LoginUserHolder.getRequired().getStoreId(), dishId);
        return Result.ok();
    }

    @Operation(summary = "上下架", description = "下架后顾客端列表立即不可见")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @RequirePerm("dish:manage")
    @PutMapping("/{dishId}/status")
    public Result<Void> updateStatus(
            @Parameter(description = "菜品 ID", required = true) @PathVariable Long dishId,
            @Parameter(description = "状态：0-下架，1-上架", required = true) @RequestParam Integer status) {
        dishService.updateStatus(LoginUserHolder.getRequired().getStoreId(), dishId, status);
        return Result.ok();
    }

    @Operation(summary = "调整排序", description = "排序值越小越靠前")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @RequirePerm("dish:manage")
    @PutMapping("/{dishId}/sort")
    public Result<Void> updateSort(
            @Parameter(description = "菜品 ID", required = true) @PathVariable Long dishId,
            @Parameter(description = "排序值", required = true) @RequestParam Integer sort) {
        dishService.updateSort(LoginUserHolder.getRequired().getStoreId(), dishId, sort);
        return Result.ok();
    }
}
