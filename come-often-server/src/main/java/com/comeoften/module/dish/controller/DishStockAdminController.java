package com.comeoften.module.dish.controller;

import com.comeoften.common.Result;
import com.comeoften.module.dish.dto.DishStockDTO;
import com.comeoften.module.dish.service.DishStockService;
import com.comeoften.module.dish.vo.DishStockVO;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * PC 后台每日库存与售罄。
 */
@Tag(name = "PC 后台-每日库存", description = "设置备货、一键售罄/开售，事务内同步 Redis 库存键")
@RestController
@RequestMapping("/api/admin/dish/stock")
@RequiredArgsConstructor
public class DishStockAdminController {

    private final DishStockService dishStockService;

    @Operation(summary = "当日库存清单", description = "未配置当日库存的菜品标记为未配置（不限量）")
    @ApiResponse(responseCode = "200", description = "返回全部上架菜品的当日库存")
    @RequirePerm("dish:manage")
    @GetMapping
    public Result<List<DishStockVO>> list() {
        return Result.ok(dishStockService.list(LoginUserHolder.getRequired().getStoreId()));
    }

    @Operation(summary = "单个菜品库存", description = "DB 的 remain 为最终事实源")
    @ApiResponse(responseCode = "200", description = "返回当日库存")
    @RequirePerm("dish:manage")
    @GetMapping("/{dishId}")
    public Result<DishStockVO> get(
            @Parameter(description = "菜品 ID", required = true) @PathVariable Long dishId) {
        return Result.ok(dishStockService.get(LoginUserHolder.getRequired().getStoreId(), dishId));
    }

    @Operation(summary = "设置备货或一键售罄", description = "soldOut=true 时余量置 0，下单校验随即拒绝")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @RequirePerm("dish:manage")
    @PutMapping
    public Result<Void> apply(@Valid @RequestBody DishStockDTO dto) {
        dishStockService.apply(LoginUserHolder.getRequired().getStoreId(), dto);
        return Result.ok();
    }

    @Operation(summary = "按 DB 重建当日 Redis 库存键", description = "供每日兜底任务与验收脚本调用")
    @ApiResponse(responseCode = "200", description = "返回重建条数")
    @RequirePerm("dish:manage")
    @PostMapping("/rebuild")
    public Result<Integer> rebuild() {
        return Result.ok(dishStockService.rebuildToday(LoginUserHolder.getRequired().getStoreId()));
    }
}
