package com.comeoften.module.bundle.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.bundle.dto.BundleSaveDTO;
import com.comeoften.module.bundle.service.GroupBundleService;
import com.comeoften.module.bundle.vo.BundleVO;
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
 * PC 后台套餐管理。权限点 bundle:manage（B1 预登记的菜单 111）。
 */
@Tag(name = "PC 后台-团购套餐", description = "套餐分页、详情、保存、上下架与删除")
@RestController
@RequestMapping("/api/admin/bundle")
@RequiredArgsConstructor
public class BundleAdminController {

    private final GroupBundleService groupBundleService;

    @Operation(summary = "套餐分页", description = "按下架 / 上架与套餐名过滤")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @RequirePerm("bundle:manage")
    @GetMapping("/page")
    public Result<PageResult<BundleVO>> page(
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "状态：0-下架，1-上架") @RequestParam(required = false) Integer status,
            @Parameter(description = "套餐名称关键词") @RequestParam(required = false) String keyword) {
        return Result.ok(groupBundleService.pageForAdmin(LoginUserHolder.getRequired().getStoreId(),
                current, size, status, keyword));
    }

    @Operation(summary = "套餐详情", description = "含明细菜品，供编辑表单回填")
    @ApiResponse(responseCode = "200", description = "返回套餐详情")
    @RequirePerm("bundle:manage")
    @GetMapping("/{bundleId}")
    public Result<BundleVO> detail(
            @Parameter(description = "套餐 ID", required = true) @PathVariable Long bundleId) {
        return Result.ok(groupBundleService.detail(LoginUserHolder.getRequired().getStoreId(),
                bundleId, false));
    }

    @Operation(summary = "保存套餐", description = "新增或整体覆盖式编辑；明细菜品必须属于本店且不能重复")
    @ApiResponse(responseCode = "200", description = "返回套餐 ID")
    @RequirePerm("bundle:manage")
    @PostMapping("/save")
    public Result<String> save(@Valid @RequestBody BundleSaveDTO dto) {
        // 雪花 ID 转成字符串再回前端，避免 JS Number 精度丢失
        return Result.ok(String.valueOf(groupBundleService.save(
                LoginUserHolder.getRequired().getStoreId(), dto)));
    }

    @Operation(summary = "套餐上下架", description = "0-下架，1-上架；下架不影响已生成订单")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @RequirePerm("bundle:manage")
    @PostMapping("/{bundleId}/status")
    public Result<Void> updateStatus(
            @Parameter(description = "套餐 ID", required = true) @PathVariable Long bundleId,
            @Parameter(description = "目标状态", required = true) @RequestParam Integer status) {
        groupBundleService.updateStatus(LoginUserHolder.getRequired().getStoreId(), bundleId, status);
        return Result.ok();
    }

    @Operation(summary = "删除套餐", description = "逻辑删除主表并清空配置明细；已购订单保留历史快照")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @RequirePerm("bundle:manage")
    @PostMapping("/{bundleId}/remove")
    public Result<Void> remove(
            @Parameter(description = "套餐 ID", required = true) @PathVariable Long bundleId) {
        groupBundleService.remove(LoginUserHolder.getRequired().getStoreId(), bundleId);
        return Result.ok();
    }
}
