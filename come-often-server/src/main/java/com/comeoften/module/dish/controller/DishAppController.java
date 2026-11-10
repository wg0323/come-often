package com.comeoften.module.dish.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.dish.dto.DishQueryDTO;
import com.comeoften.module.dish.dto.DishQuoteDTO;
import com.comeoften.module.dish.dto.DishSearchDTO;
import com.comeoften.module.dish.dto.OrderCheckDTO;
import com.comeoften.module.dish.service.DishCategoryService;
import com.comeoften.module.dish.service.DishOrderService;
import com.comeoften.module.dish.service.DishQueryService;
import com.comeoften.module.dish.service.DishRecommendService;
import com.comeoften.module.dish.service.DishSearchService;
import com.comeoften.module.dish.vo.DishCategoryVO;
import com.comeoften.module.dish.vo.DishDetailVO;
import com.comeoften.module.dish.vo.DishListVO;
import com.comeoften.module.dish.vo.DishQuoteVO;
import com.comeoften.module.dish.vo.OrderCheckVO;
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
import java.util.List;

/**
 * 顾客端菜品展示。门店 ID 一律由服务端取当前唯一门店并覆盖请求值，避免跨店越权。
 */
@Tag(name = "顾客端-菜品", description = "分类、列表、详情、金额试算与下单前可售性校验")
@RestController
@RequestMapping("/api/app/dish")
@RequiredArgsConstructor
public class DishAppController {

    private final DishQueryService dishQueryService;

    private final DishCategoryService dishCategoryService;

    private final DishOrderService dishOrderService;

    private final DishSearchService dishSearchService;

    private final DishRecommendService dishRecommendService;

    private final StoreService storeService;

    @Operation(summary = "分类列表", description = "只返回展示中的分类，预定类显示为「需提前预定」，附带上架菜品数")
    @ApiResponse(responseCode = "200", description = "返回分类列表")
    @GetMapping("/categories")
    public Result<List<DishCategoryVO>> categories() {
        return Result.ok(dishCategoryService.listForApp(storeService.defaultStoreId()));
    }

    @Operation(summary = "菜品列表", description = "只返回当前门店的上架菜品；下架菜品对本端不可见。"
            + "关键词仅右模糊菜名，供分类页使用；跨字段搜索请用 /dish/search")
    @ApiResponse(responseCode = "200", description = "返回菜品列表")
    @GetMapping("/list")
    public Result<List<DishListVO>> list(
            @Parameter(description = "分类 ID，为空表示全部") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "关键词，右模糊匹配菜名") @RequestParam(required = false) String keyword,
            @Parameter(description = "只查预定菜") @RequestParam(required = false) Boolean preorder,
            @Parameter(description = "只查本店招牌") @RequestParam(required = false) Boolean signature) {
        DishQueryDTO query = new DishQueryDTO();
        query.setStoreId(storeService.defaultStoreId());
        query.setCategoryId(categoryId);
        query.setKeyword(keyword);
        query.setPreorder(preorder);
        query.setSignature(signature);
        return Result.ok(dishQueryService.list(query));
    }

    @Operation(summary = "搜索菜品", description = "关键词右模糊命中菜名、副标题、描述与标签；"
            + "排序为菜名 &gt; 标签 &gt; 副标题 &gt; 描述，同档招牌优先；关键词为空返回空分页")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @GetMapping("/search")
    public Result<PageResult<DishListVO>> search(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数，最大 20") @RequestParam(defaultValue = "10") Long size) {
        DishSearchDTO query = new DishSearchDTO();
        query.setStoreId(storeService.defaultStoreId());
        query.setKeyword(keyword);
        query.setCurrent(current);
        query.setSize(size);
        return Result.ok(dishSearchService.search(query));
    }

    @Operation(summary = "历史订单推荐", description = "取本店最近 3 笔已完成订单的菜品，去重并排除下架与售罄；"
            + "未登录返回 401，无历史返回空列表，页面据此隐藏模块")
    @ApiResponse(responseCode = "200", description = "返回菜品列表")
    @ApiResponse(responseCode = "401", description = "未登录")
    @GetMapping("/recommend")
    public Result<List<DishListVO>> recommend() {
        return Result.ok(dishRecommendService.listFromHistory(
                LoginUserHolder.getRequired().getUserId(), storeService.defaultStoreId()));
    }

    @Operation(summary = "菜品详情", description = "含多图、规格组、加料、标签与预定菜角标文案；非本店或下架返回 20002 / 20004")
    @ApiResponse(responseCode = "200", description = "返回菜品详情")
    @GetMapping("/{dishId}")
    public Result<DishDetailVO> detail(
            @Parameter(description = "菜品 ID", required = true) @PathVariable Long dishId) {
        return Result.ok(dishQueryService.detail(dishId, storeService.defaultStoreId()));
    }

    @Operation(summary = "金额试算", description = "页面只提交已选项，规格与加料加价由服务端汇总；门店由服务端覆盖")
    @ApiResponse(responseCode = "200", description = "返回单位价与行金额")
    @ApiResponse(responseCode = "200", description = "规格或加料非法时 body.code=20006 / 20007")
    @PostMapping("/quote")
    public Result<DishQuoteVO> quote(@Valid @RequestBody DishQuoteDTO dto) {
        dto.setStoreId(storeService.defaultStoreId());
        dto.setMemberId(LoginUserHolder.getRequired().getUserId());
        return Result.ok(dishOrderService.quote(dto));
    }

    @Operation(summary = "下单前可售性校验", description = "M3 下单复用同一 Service；售罄返回 20005、下架返回 20004")
    @ApiResponse(responseCode = "200", description = "校验通过，返回整单金额与定金")
    @PostMapping("/order-check")
    public Result<OrderCheckVO> check(@Valid @RequestBody OrderCheckDTO dto) {
        dto.setStoreId(storeService.defaultStoreId());
        dto.setMemberId(LoginUserHolder.getRequired().getUserId());
        return Result.ok(dishOrderService.check(dto));
    }
}
