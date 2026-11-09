package com.comeoften.module.home.controller;

import com.comeoften.common.Result;
import com.comeoften.module.home.dto.BannerSaveDTO;
import com.comeoften.module.home.dto.NoticeSaveDTO;
import com.comeoften.module.home.service.HomeService;
import com.comeoften.module.home.vo.BannerVO;
import com.comeoften.module.home.vo.NoticeVO;
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
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * PC 后台运营位管理：轮播与公告。
 */
@Tag(name = "PC 后台-首页运营位", description = "轮播与公告 CRUD，权重越大越靠前")
@RestController
@RequestMapping("/api/admin/home")
@RequiredArgsConstructor
public class HomeAdminController {

    private final HomeService homeService;

    @Operation(summary = "轮播列表", description = "只返回当前生效记录，按权重降序")
    @ApiResponse(responseCode = "200", description = "返回轮播列表")
    @RequirePerm("dish:manage")
    @GetMapping("/banners")
    public Result<List<BannerVO>> listBanners() {
        return Result.ok(homeService.listBanners(storeId()));
    }

    @Operation(summary = "新增轮播", description = "图片 objectKey 由上传接口返回")
    @ApiResponse(responseCode = "200", description = "返回轮播 ID")
    @RequirePerm("dish:manage")
    @PostMapping("/banners")
    public Result<String> createBanner(@Valid @RequestBody BannerSaveDTO dto) {
        dto.setId(null);
        return Result.ok(String.valueOf(homeService.saveBanner(dto, storeId())));
    }

    @Operation(summary = "编辑轮播", description = "可改权重、跳转与生效时间")
    @ApiResponse(responseCode = "200", description = "返回轮播 ID")
    @RequirePerm("dish:manage")
    @PutMapping("/banners/{bannerId}")
    public Result<String> updateBanner(
            @Parameter(description = "轮播 ID", required = true) @PathVariable Long bannerId,
            @Valid @RequestBody BannerSaveDTO dto) {
        dto.setId(bannerId);
        return Result.ok(String.valueOf(homeService.saveBanner(dto, storeId())));
    }

    @Operation(summary = "删除轮播", description = "逻辑删除")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @RequirePerm("dish:manage")
    @DeleteMapping("/banners/{bannerId}")
    public Result<Void> removeBanner(
            @Parameter(description = "轮播 ID", required = true) @PathVariable Long bannerId) {
        homeService.removeBanner(bannerId, storeId());
        return Result.ok();
    }

    @Operation(summary = "公告列表", description = "单行滚动，不做弹窗")
    @ApiResponse(responseCode = "200", description = "返回公告列表")
    @RequirePerm("dish:manage")
    @GetMapping("/notices")
    public Result<List<NoticeVO>> listNotices() {
        return Result.ok(homeService.listNotices(storeId()));
    }

    @Operation(summary = "新增公告", description = "生效时间留空表示长期有效")
    @ApiResponse(responseCode = "200", description = "返回公告 ID")
    @RequirePerm("dish:manage")
    @PostMapping("/notices")
    public Result<String> createNotice(@Valid @RequestBody NoticeSaveDTO dto) {
        dto.setId(null);
        return Result.ok(String.valueOf(homeService.saveNotice(dto, storeId())));
    }

    @Operation(summary = "编辑公告")
    @ApiResponse(responseCode = "200", description = "返回公告 ID")
    @RequirePerm("dish:manage")
    @PutMapping("/notices/{noticeId}")
    public Result<String> updateNotice(
            @Parameter(description = "公告 ID", required = true) @PathVariable Long noticeId,
            @Valid @RequestBody NoticeSaveDTO dto) {
        dto.setId(noticeId);
        return Result.ok(String.valueOf(homeService.saveNotice(dto, storeId())));
    }

    @Operation(summary = "删除公告", description = "逻辑删除")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @RequirePerm("dish:manage")
    @DeleteMapping("/notices/{noticeId}")
    public Result<Void> removeNotice(
            @Parameter(description = "公告 ID", required = true) @PathVariable Long noticeId) {
        homeService.removeNotice(noticeId, storeId());
        return Result.ok();
    }

    private Long storeId() {
        return LoginUserHolder.getRequired().getStoreId();
    }
}
