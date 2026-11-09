package com.comeoften.module.store.controller;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.Result;
import com.comeoften.module.store.dto.StoreTableDTO;
import com.comeoften.module.store.dto.StoreUpdateDTO;
import com.comeoften.module.store.service.StoreService;
import com.comeoften.module.store.vo.StoreVO;
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
import java.util.Collections;
import java.util.List;

/**
 * PC 后台门店管理。门店信息与营业时间一次提交，桌台单独维护。
 */
@Tag(name = "PC 后台-门店管理", description = "门店信息、一周营业时间与桌台配置")
@RestController
@RequestMapping("/api/admin/store")
@RequiredArgsConstructor
public class StoreAdminController {

    private final StoreService storeService;

    @Operation(summary = "门店列表", description = "含营业时间与桌台")
    @ApiResponse(responseCode = "200", description = "返回门店列表")
    @RequirePerm("store:manage")
    @GetMapping("/list")
    public Result<List<StoreVO>> list() {
        Long ownStoreId = LoginUserHolder.getRequired().getStoreId();
        return Result.ok(Collections.singletonList(storeService.detail(ownStoreId)));
    }

    @Operation(summary = "门店详情", description = "含营业时间与桌台")
    @ApiResponse(responseCode = "200", description = "返回门店详情")
    @RequirePerm("store:manage")
    @GetMapping("/{storeId}")
    public Result<StoreVO> detail(
            @Parameter(description = "门店 ID", required = true) @PathVariable Long storeId) {
        assertOwnStore(storeId);
        return Result.ok(storeService.detail(storeId));
    }

    @Operation(summary = "更新门店", description = "businessHours 非空时按星期 upsert，不做物理删除")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @RequirePerm("store:manage")
    @PutMapping("/{storeId}")
    public Result<Void> update(
            @Parameter(description = "门店 ID", required = true) @PathVariable Long storeId,
            @Valid @RequestBody StoreUpdateDTO dto) {
        assertOwnStore(storeId);
        storeService.update(storeId, dto);
        return Result.ok();
    }

    @Operation(summary = "新增桌台", description = "桌号与容量全部由数据配置")
    @ApiResponse(responseCode = "200", description = "返回桌台 ID")
    @RequirePerm("store:manage")
    @PostMapping("/{storeId}/tables")
    public Result<String> createTable(
            @Parameter(description = "门店 ID", required = true) @PathVariable Long storeId,
            @Valid @RequestBody StoreTableDTO dto) {
        assertOwnStore(storeId);
        dto.setId(null);
        return Result.ok(String.valueOf(storeService.saveTable(storeId, dto)));
    }

    @Operation(summary = "编辑桌台", description = "可改桌型、容量与状态")
    @ApiResponse(responseCode = "200", description = "返回桌台 ID")
    @RequirePerm("store:manage")
    @PutMapping("/{storeId}/tables/{tableId}")
    public Result<String> updateTable(
            @Parameter(description = "门店 ID", required = true) @PathVariable Long storeId,
            @Parameter(description = "桌台 ID", required = true) @PathVariable Long tableId,
            @Valid @RequestBody StoreTableDTO dto) {
        assertOwnStore(storeId);
        dto.setId(tableId);
        return Result.ok(String.valueOf(storeService.saveTable(storeId, dto)));
    }

    @Operation(summary = "删除桌台", description = "逻辑删除；uk_store_table_no 唯一索引下同名桌号删除后不可重建")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @RequirePerm("store:manage")
    @DeleteMapping("/{storeId}/tables/{tableId}")
    public Result<Void> removeTable(
            @Parameter(description = "门店 ID", required = true) @PathVariable Long storeId,
            @Parameter(description = "桌台 ID", required = true) @PathVariable Long tableId) {
        assertOwnStore(storeId);
        storeService.removeTable(storeId, tableId);
        return Result.ok();
    }

    private void assertOwnStore(Long storeId) {
        Long ownStoreId = LoginUserHolder.getRequired().getStoreId();
        if (!storeId.equals(ownStoreId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能操作其他门店");
        }
    }
}
