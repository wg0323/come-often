package com.comeoften.module.cart.controller;

import com.comeoften.common.Result;
import com.comeoften.module.cart.dto.CartAddDTO;
import com.comeoften.module.cart.dto.CartQuantityDTO;
import com.comeoften.module.cart.service.CartService;
import com.comeoften.module.cart.vo.CartVO;
import com.comeoften.module.store.service.StoreService;
import com.comeoften.security.LoginUserHolder;
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

/**
 * 顾客端购物车。会员 ID 取登录态、门店 ID 由服务端覆盖，避免跨店越权。
 */
@Tag(name = "顾客端-购物车", description = "购物车增删改查；行金额由服务端试算，页面不计算")
@RestController
@RequestMapping("/api/app/cart")
@RequiredArgsConstructor
public class CartAppController {

    private final CartService cartService;

    private final StoreService storeService;

    @Operation(summary = "购物车列表", description = "每行金额取服务端试算结果；失效行标记 invalid")
    @ApiResponse(responseCode = "200", description = "返回购物车")
    @GetMapping
    public Result<CartVO> list() {
        return Result.ok(cartService.list(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId()));
    }

    @Operation(summary = "加入购物车", description = "复用 M2 的 quote 与 assertOrderable 校验；相同 SKU 累加份数")
    @ApiResponse(responseCode = "200", description = "返回购物车行 ID")
    @PostMapping
    public Result<String> add(@Valid @RequestBody CartAddDTO dto) {
        return Result.ok(cartService.add(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), dto));
    }

    @Operation(summary = "修改份数", description = "份数必须大于 0；删除请调用 DELETE")
    @ApiResponse(responseCode = "200", description = "修改成功")
    @PutMapping("/{itemId}/quantity")
    public Result<Void> updateQuantity(
            @Parameter(description = "购物车行 ID", required = true) @PathVariable Long itemId,
            @Valid @RequestBody CartQuantityDTO dto) {
        cartService.updateQuantity(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), itemId, dto.getQuantity());
        return Result.ok();
    }

    @Operation(summary = "删除购物车行", description = "逻辑删除，仅能删本人本店的行")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{itemId}")
    public Result<Void> remove(
            @Parameter(description = "购物车行 ID", required = true) @PathVariable Long itemId) {
        cartService.remove(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), itemId);
        return Result.ok();
    }

    @Operation(summary = "清空购物车", description = "下单成功后由服务端调用，也允许顾客端主动清空")
    @ApiResponse(responseCode = "200", description = "清空成功")
    @DeleteMapping
    public Result<Void> clear() {
        cartService.clear(LoginUserHolder.getRequired().getUserId(), storeService.defaultStoreId());
        return Result.ok();
    }
}
