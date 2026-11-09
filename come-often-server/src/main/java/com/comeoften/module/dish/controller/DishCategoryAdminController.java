package com.comeoften.module.dish.controller;

import com.comeoften.common.Result;
import com.comeoften.module.dish.dto.CategorySaveDTO;
import com.comeoften.module.dish.service.DishCategoryService;
import com.comeoften.module.dish.vo.DishCategoryVO;
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
import java.util.List;

/**
 * PC 后台分类管理。
 */
@Tag(name = "PC 后台-菜品分类", description = "分类 CRUD 与排序；后台保留「预定」原名")
@RestController
@RequestMapping("/api/admin/dish/category")
@RequiredArgsConstructor
public class DishCategoryAdminController {

    private final DishCategoryService dishCategoryService;

    @Operation(summary = "分类列表", description = "后台展示原始分类名与上架菜品数")
    @ApiResponse(responseCode = "200", description = "返回分类列表")
    @RequirePerm("dish:manage")
    @GetMapping
    public Result<List<DishCategoryVO>> list() {
        return Result.ok(dishCategoryService.listForAdmin(LoginUserHolder.getRequired().getStoreId()));
    }

    @Operation(summary = "新增分类", description = "同门店下分类名唯一")
    @ApiResponse(responseCode = "200", description = "返回新分类 ID")
    @RequirePerm("dish:manage")
    @PostMapping
    public Result<String> create(@Valid @RequestBody CategorySaveDTO dto) {
        dto.setId(null);
        return Result.ok(String.valueOf(dishCategoryService.save(LoginUserHolder.getRequired().getStoreId(), dto)));
    }

    @Operation(summary = "编辑分类", description = "分类名、排序、是否展示")
    @ApiResponse(responseCode = "200", description = "返回分类 ID")
    @RequirePerm("dish:manage")
    @PutMapping("/{categoryId}")
    public Result<String> update(
            @Parameter(description = "分类 ID", required = true) @PathVariable Long categoryId,
            @Valid @RequestBody CategorySaveDTO dto) {
        dto.setId(categoryId);
        return Result.ok(String.valueOf(dishCategoryService.save(LoginUserHolder.getRequired().getStoreId(), dto)));
    }

    @Operation(summary = "删除分类", description = "分类下仍有菜品时拒绝删除")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @RequirePerm("dish:manage")
    @DeleteMapping("/{categoryId}")
    public Result<Void> remove(
            @Parameter(description = "分类 ID", required = true) @PathVariable Long categoryId) {
        dishCategoryService.remove(LoginUserHolder.getRequired().getStoreId(), categoryId);
        return Result.ok();
    }

    @Operation(summary = "调整分类排序", description = "排序值越小越靠前")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @RequirePerm("dish:manage")
    @PutMapping("/{categoryId}/sort")
    public Result<Void> updateSort(
            @Parameter(description = "分类 ID", required = true) @PathVariable Long categoryId,
            @Parameter(description = "排序值", required = true) @RequestParam Integer sort) {
        dishCategoryService.updateSort(LoginUserHolder.getRequired().getStoreId(), categoryId, sort);
        return Result.ok();
    }
}
