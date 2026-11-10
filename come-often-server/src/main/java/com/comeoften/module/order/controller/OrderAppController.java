package com.comeoften.module.order.controller;

import com.comeoften.common.PageResult;
import com.comeoften.common.Result;
import com.comeoften.module.order.dto.OrderCreateDTO;
import com.comeoften.module.order.dto.OrderPayDTO;
import com.comeoften.module.order.service.OrderService;
import com.comeoften.module.order.vo.OrderPreviewVO;
import com.comeoften.module.order.vo.OrderQrCodeVO;
import com.comeoften.module.order.vo.OrderSummaryVO;
import com.comeoften.module.order.vo.OrderVO;
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
 * 顾客端订单。会员 ID 取登录态、门店 ID 由服务端覆盖，金额一律由服务端试算。
 */
@Tag(name = "顾客端-订单", description = "确认订单、下单、模拟支付、取消与订单查询")
@RestController
@RequestMapping("/api/app/order")
@RequiredArgsConstructor
public class OrderAppController {

    private final OrderService orderService;

    private final StoreService storeService;

    @Operation(summary = "确认订单页", description = "金额明细直接来自 /dish/order-check；含预定菜返回 24 小时最早到店时间")
    @ApiResponse(responseCode = "200", description = "返回确认页数据")
    @PostMapping("/preview")
    public Result<OrderPreviewVO> preview(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.ok(orderService.preview(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), dto));
    }

    @Operation(summary = "提交订单", description = "复用 M2 校验与金额；含预定菜强制 DINE_IN，生成取餐码或到店核销码")
    @ApiResponse(responseCode = "200", description = "返回订单详情")
    @PostMapping
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto) {
        return Result.ok(orderService.create(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), dto));
    }

    @Operation(summary = "我的订单列表", description = "按状态过滤，按下单时间倒序")
    @ApiResponse(responseCode = "200", description = "返回分页结果")
    @GetMapping
    public Result<PageResult<OrderSummaryVO>> page(
            @Parameter(description = "当前页，从 1 开始") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "状态过滤") @RequestParam(required = false) String status) {
        return Result.ok(orderService.pageForApp(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), current, size, status));
    }

    @Operation(summary = "订单详情", description = "含金额快照、明细、取餐码与状态日志；仅本人可查")
    @ApiResponse(responseCode = "200", description = "返回订单详情")
    @GetMapping("/{orderNo}")
    public Result<OrderVO> detail(
            @Parameter(description = "订单号", required = true) @PathVariable String orderNo) {
        return Result.ok(orderService.detailForApp(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), orderNo));
    }

    @Operation(summary = "取餐码二维码", description = "仅本人可查；返回码串与 PNG 的 base64，端上直接用 image 渲染")
    @ApiResponse(responseCode = "200", description = "返回取餐码与二维码图片")
    @GetMapping("/{orderNo}/qrcode")
    public Result<OrderQrCodeVO> qrcode(
            @Parameter(description = "订单号", required = true) @PathVariable String orderNo) {
        return Result.ok(orderService.pickupQrCode(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), orderNo));
    }

    @Operation(summary = "发起支付",
            description = "M4：走 PayGateway 预下单，Mock 网关同步确认收款后由回调入账；含预定菜付定金，普通单付实付额")
    @ApiResponse(responseCode = "200", description = "返回支付后的订单详情")
    @PostMapping("/{orderNo}/pay")
    public Result<OrderVO> pay(
            @Parameter(description = "订单号", required = true) @PathVariable String orderNo,
            @Valid @RequestBody OrderPayDTO dto) {
        return Result.ok(orderService.pay(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), orderNo, dto));
    }

    @Operation(summary = "取消订单", description = "CREATED / PAID 可取消，取消后回补 DB 与 Redis 库存")
    @ApiResponse(responseCode = "200", description = "返回取消后的订单详情")
    @PostMapping("/{orderNo}/cancel")
    public Result<OrderVO> cancel(
            @Parameter(description = "订单号", required = true) @PathVariable String orderNo) {
        return Result.ok(orderService.cancel(LoginUserHolder.getRequired().getUserId(),
                storeService.defaultStoreId(), orderNo));
    }
}
