package com.comeoften.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.common.OrderKeys;
import com.comeoften.common.PageResult;
import com.comeoften.common.StockKeys;
import com.comeoften.common.util.QrCodeUtils;
import com.comeoften.module.balance.service.BalanceService;
import com.comeoften.module.cart.entity.CartItem;
import com.comeoften.module.cart.mapper.CartItemMapper;
import com.comeoften.module.cart.util.CartSkuKeys;
import com.comeoften.module.coupon.service.UserCouponService;
import com.comeoften.module.coupon.vo.CouponSelectionVO;
import com.comeoften.module.dish.converter.DishConverter;
import com.comeoften.module.dish.dto.OrderCheckDTO;
import com.comeoften.module.dish.entity.Dish;
import com.comeoften.module.dish.entity.DishAddon;
import com.comeoften.module.dish.entity.DishImage;
import com.comeoften.module.dish.entity.DishSpecOption;
import com.comeoften.module.dish.entity.DishStock;
import com.comeoften.module.dish.mapper.DishAddonMapper;
import com.comeoften.module.dish.mapper.DishImageMapper;
import com.comeoften.module.dish.mapper.DishMapper;
import com.comeoften.module.dish.mapper.DishSpecOptionMapper;
import com.comeoften.module.dish.mapper.DishStockMapper;
import com.comeoften.module.dish.service.DishOrderService;
import com.comeoften.module.dish.util.DishConstants;
import com.comeoften.module.dish.vo.OrderCheckVO;
import com.comeoften.module.order.constant.OperatorType;
import com.comeoften.module.order.constant.OrderStatus;
import com.comeoften.module.order.constant.OrderStatusTransition;
import com.comeoften.module.order.constant.OrderType;
import com.comeoften.module.order.converter.OrderConverter;
import com.comeoften.module.order.dto.OrderCreateDTO;
import com.comeoften.module.order.dto.OrderPayDTO;
import com.comeoften.module.order.entity.OrderItem;
import com.comeoften.module.order.entity.OrderStatusLog;
import com.comeoften.module.order.entity.TradeOrder;
import com.comeoften.module.order.mapper.OrderItemMapper;
import com.comeoften.module.order.mapper.OrderStatusLogMapper;
import com.comeoften.module.order.mapper.TradeOrderMapper;
import com.comeoften.module.order.service.OrderService;
import com.comeoften.module.order.util.OrderAmountCalculator;
import com.comeoften.module.order.util.PickupCodeGenerator;
import com.comeoften.module.order.vo.OrderPreviewVO;
import com.comeoften.module.order.vo.OrderQrCodeVO;
import com.comeoften.module.order.vo.OrderSummaryVO;
import com.comeoften.module.order.vo.OrderVO;
import com.comeoften.module.payment.service.PaymentService;
import com.comeoften.module.points.dto.PointsExchangeDTO;
import com.comeoften.module.points.service.PointsAccountService;
import com.comeoften.module.points.service.PointsLevelService;
import com.comeoften.module.points.util.MemberDiscountCalculator;
import com.comeoften.module.review.service.ReviewService;
import com.comeoften.module.store.entity.StoreTable;
import com.comeoften.module.store.mapper.StoreTableMapper;
import com.comeoften.module.store.service.StoreService;
import com.comeoften.mq.MessageProducer;
import com.comeoften.mq.RabbitConst;
import com.comeoften.mq.message.MqEnvelope;
import com.comeoften.mq.message.OrderEventMessage;
import com.comeoften.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单主流程。
 *
 * <p>金额唯一口径来自 M2 的 DishOrderService.check；库存遵循「Redis Lua 预扣 → DB 条件扣减 →
 * 失败回补 Redis」，DB 的 remain 是最终事实源；状态迁移统一走 OrderStatusTransition，
 * 每次迁移在同一事务内写 order_status_log 与 mq_message(PENDING)，提交后投递。</p>
 *
 * <p>预定菜硬校验在 resolveOrderRules：提前 24 小时、强制 DINE_IN、200 元定金整单抵扣。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String IMAGE_TYPE_LIST = "LIST";

    private static final String IMAGE_TYPE_THUMB = "THUMB";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final long CODE_KEY_TTL_HOURS = 48L;

    private static final int MAX_CODE_RETRY = 5;

    private static final long MAX_REPLAY_DELAY_SECONDS = 3600L;

    /**
     * 取餐码二维码边长（像素）。码串只有 4 位，240 足够商家端扫清。
     */
    private static final int QR_IMAGE_SIZE = 240;

    /**
     * 组合支付后在线应付额的下限。与 M6 积分抵现同一口径：不允许产生 0 元订单，
     * 否则支付网关、定金与退款上限三条链路同时失去意义。
     */
    private static final BigDecimal MIN_ONLINE_PAY_AMOUNT = new BigDecimal("0.01");

    private final TradeOrderMapper tradeOrderMapper;

    private final OrderItemMapper orderItemMapper;

    private final OrderStatusLogMapper orderStatusLogMapper;

    private final PaymentService paymentService;

    private final CartItemMapper cartItemMapper;

    private final DishMapper dishMapper;

    private final DishStockMapper dishStockMapper;

    private final DishImageMapper dishImageMapper;

    private final DishSpecOptionMapper dishSpecOptionMapper;

    private final DishAddonMapper dishAddonMapper;

    private final DishOrderService dishOrderService;

    private final StockService stockService;

    private final MessageProducer messageProducer;

    private final UserCouponService userCouponService;

    private final ReviewService reviewService;

    private final PointsAccountService pointsAccountService;

    private final PointsLevelService pointsLevelService;

    private final BalanceService balanceService;

    private final StringRedisTemplate stringRedisTemplate;

    private final StoreTableMapper storeTableMapper;

    private final StoreService storeService;

    @Value("${comeoften.order.pay-timeout-seconds:900}")
    private long payTimeoutSeconds;

    @Override
    public OrderPreviewVO preview(Long memberId, Long storeId, OrderCreateDTO dto) {
        Selection selection = prepare(memberId, storeId, dto);
        String earliest = selection.containsPreorder
                ? TIME_FORMATTER.format(LocalDateTime.now().plusHours(selection.preorderLeadHours)) : "";
        BigDecimal discount = discountOf(selection);
        OrderPreviewVO vo = OrderConverter.toPreviewVO(selection.check, selection.containsPreorder,
                selection.orderType, selection.forcedDineIn, selection.preorderLeadHours,
                earliest, selection.containsPreorder ? DishConstants.PREORDER_NOTE : "",
                discount, selection.pointsAmount, selection.memberDiscountAmount,
                selection.balanceDeductAmount);
        if (selection.coupon != null) {
            vo.setCouponId(selection.coupon.getUserCouponId());
            vo.setCouponName(selection.coupon.getTemplateName());
        }
        vo.setPointsUsed(selection.pointsUsed);
        vo.setPointsBalance(pointsAccountService.balanceOf(memberId));
        vo.setMaxUsablePoints(pointsAccountService.maxUsablePoints(memberId,
                selection.check.getTotalAmount().subtract(discount)
                        .subtract(selection.memberDiscountAmount)));
        vo.setPointsExchangeRate(pointsAccountService.rule().getExchangeRate());
        vo.setBalanceAvailable(balanceService.availableOf(memberId));
        vo.setAvailableCoupons(userCouponService.listUsable(memberId, storeId,
                selection.check.getTotalAmount(), selection.dishAmounts));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO create(Long memberId, Long storeId, OrderCreateDTO dto) {
        storeService.requireOpen(storeId);
        Selection selection = prepare(memberId, storeId, dto);
        Long tableId = resolveTableId(storeId, selection.orderType, dto.getTableId());
        List<Deduction> deductions = preDeduct(storeId, selection.quantityByDish);
        // 事务一旦回滚就回补 Redis；DB 回滚由 Spring 负责，两条路径不会重复回补
        registerRollbackCompensation(deductions);
        for (Deduction deduction : deductions) {
            if (dishStockMapper.deduct(storeId, deduction.dishId, deduction.date, deduction.quantity) == 0) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, "并发下库存不足，请调整份数后重试");
            }
        }
        LocalDate bizDate = LocalDate.now();
        String code = generateCode(storeId, bizDate, selection.orderType);
        TradeOrder order = new TradeOrder();
        order.setOrderNo(generateOrderNo());
        order.setStoreId(storeId);
        order.setMemberId(memberId);
        order.setOrderType(selection.orderType.name());
        order.setStatus(OrderStatus.CREATED.name());
        BigDecimal discount = discountOf(selection);
        BigDecimal pointsAmount = selection.pointsAmount;
        BigDecimal payAmount = OrderAmountCalculator.pay(selection.check.getTotalAmount(), discount,
                pointsAmount, selection.memberDiscountAmount, selection.balanceDeductAmount);
        order.setTotalAmount(selection.check.getTotalAmount());
        order.setDiscountAmount(discount);
        order.setPointsAmount(pointsAmount);
        order.setMemberDiscount(selection.memberDiscountAmount);
        order.setBalanceAmount(selection.balanceDeductAmount);
        // 定金是券后实付的一部分：券把整单压到 200 以下时按券后金额收，不会出现定金大于实付
        order.setDepositAmount(OrderAmountCalculator.deposit(payAmount, selection.containsPreorder));
        order.setPayAmount(payAmount);
        order.setCouponId(selection.coupon == null ? null : selection.coupon.getUserCouponId());
        order.setPointsUsed(selection.pointsUsed);
        order.setPickupCode(code);
        order.setTableId(tableId);
        order.setBizDate(bizDate);
        order.setPickupTime(selection.pickupTime);
        order.setDineInTime(selection.dineInTime);
        order.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        try {
            tradeOrderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "取餐码冲突，请重试下单");
        }
        if (selection.coupon != null) {
            // 券的锁定同样用带旧状态条件的 UPDATE：并发下单只有一单能占到这张券
            userCouponService.lockForOrder(memberId, selection.coupon.getUserCouponId(), order.getId());
        }
        // 积分抵现先冻结：余额在冻结时扣减，支付成功转实扣，取消 / 超时写反向流水回滚
        pointsAccountService.freezeForOrder(memberId, order.getOrderNo(), selection.pointsUsed);
        // 储值余额在下单事务内实扣（余额是支付手段而非优惠），取消 / 超时写 #ROLLBACK 冲销
        balanceService.deductForOrder(memberId, order.getOrderNo(), selection.balanceDeductAmount);
        insertItems(order, selection);
        writeLog(order, null, OrderStatus.CREATED, OperatorType.MEMBER, memberId, "订单创建");
        cartItemMapper.deleteBatchIds(selection.cartItems.stream()
                .map(CartItem::getId).collect(Collectors.toList()));
        publishStatus(order, null, OrderStatus.CREATED);
        publishTimeout(order, TimeUnit.SECONDS.toMillis(payTimeoutSeconds));
        log.info("订单已创建: orderNo={}, orderType={}, pickupCode={}, totalAmount={}, depositAmount={}",
                order.getOrderNo(), order.getOrderType(), code, order.getTotalAmount(),
                order.getDepositAmount());
        return toVO(order);
    }

    /**
     * 积分兑换下单：只换一道菜（可带规格 / 加料），整单金额 = 所需积分 / 兑换比例。
     * 订单直接落到 PAID——没有在线支付环节，也没有定金；不参与券、会员折扣与余额抵扣，
     * 否则「兑换」与「抵现」两套语义会被混在一起。取消时由 doCancel 走 {@code #EXCHANGE_ROLLBACK} 退回积分。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO exchange(Long memberId, Long storeId, PointsExchangeDTO dto) {
        storeService.requireOpen(storeId);
        OrderCheckDTO checkDTO = new OrderCheckDTO();
        checkDTO.setStoreId(storeId);
        checkDTO.setMemberId(memberId);
        OrderCheckDTO.ItemDTO row = new OrderCheckDTO.ItemDTO();
        row.setDishId(dto.getDishId());
        row.setQuantity(dto.getQuantity() == null ? 1 : dto.getQuantity());
        row.setSpecOptionIds(dto.getSpecOptionIds());
        row.setAddonIds(dto.getAddonIds());
        checkDTO.setItems(Collections.singletonList(row));
        OrderCheckVO check = dishOrderService.check(checkDTO);
        Dish dish = dishMapper.selectById(dto.getDishId());
        if (dish == null || !storeId.equals(dish.getStoreId())) {
            throw new BizException(ErrorCode.DISH_NOT_FOUND);
        }
        if (DishConverter.isPreorder(dish)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "预定菜暂不支持积分兑换");
        }
        BigDecimal amount = check.getTotalAmount();
        if (amount.signum() <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "兑换金额必须大于 0");
        }
        int points = pointsAccountService.pointsRequired(amount);
        Map<Long, Integer> quantityByDish = new LinkedHashMap<>();
        quantityByDish.put(dish.getId(), row.getQuantity());
        List<Deduction> deductions = preDeduct(storeId, quantityByDish);
        registerRollbackCompensation(deductions);
        for (Deduction deduction : deductions) {
            if (dishStockMapper.deduct(storeId, deduction.dishId, deduction.date, deduction.quantity) == 0) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, "并发下库存不足，请稍后重试");
            }
        }
        LocalDate bizDate = LocalDate.now();
        String code = generateCode(storeId, bizDate, OrderType.PICKUP);
        TradeOrder order = new TradeOrder();
        order.setOrderNo(generateOrderNo());
        order.setStoreId(storeId);
        order.setMemberId(memberId);
        order.setOrderType(OrderType.PICKUP.name());
        order.setStatus(OrderStatus.PAID.name());
        order.setTotalAmount(amount);
        order.setDiscountAmount(BigDecimal.ZERO.setScale(2));
        order.setPointsAmount(amount);
        order.setMemberDiscount(BigDecimal.ZERO.setScale(2));
        order.setBalanceAmount(BigDecimal.ZERO.setScale(2));
        order.setDepositAmount(BigDecimal.ZERO.setScale(2));
        order.setPayAmount(BigDecimal.ZERO.setScale(2));
        order.setPointsUsed(points);
        order.setPickupCode(code);
        order.setBizDate(bizDate);
        order.setRemark("积分兑换，扣减 " + points + " 积分");
        order.setPaidTime(LocalDateTime.now());
        try {
            tradeOrderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "取餐码冲突，请重试下单");
        }
        insertExchangeItems(order, check, dto.getSpecOptionIds(), dto.getAddonIds(),
                loadImageOf(dish.getId()));
        pointsAccountService.redeemForExchange(memberId, order.getOrderNo(), points);
        writeLog(order, null, OrderStatus.PAID, OperatorType.MEMBER, memberId,
                "积分兑换下单，扣减 " + points + " 积分");
        publishStatus(order, null, OrderStatus.PAID);
        log.info("积分兑换订单已创建: orderNo={}, dishId={}, amount={}, points={}",
                order.getOrderNo(), dish.getId(), amount, points);
        return toVO(order);
    }

    @Override
    public OrderVO detailForApp(Long memberId, Long storeId, String orderNo) {
        TradeOrder order = requireOwned(memberId, storeId, orderNo);
        OrderVO vo = toVO(order);
        boolean reviewed = reviewService.reviewedOrderIds(memberId,
                Collections.singletonList(order.getId())).contains(order.getId());
        boolean completed = OrderStatus.COMPLETED.name().equals(order.getStatus());
        vo.setReviewed(reviewed);
        vo.setCanReview(completed && !reviewed);
        return vo;
    }

    @Override
    public OrderQrCodeVO pickupQrCode(Long memberId, Long storeId, String orderNo) {
        TradeOrder order = requireOwned(memberId, storeId, orderNo);
        OrderQrCodeVO vo = new OrderQrCodeVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setPickupCode(order.getPickupCode());
        try {
            vo.setQrImageBase64(QrCodeUtils.pngBase64(order.getPickupCode(), QR_IMAGE_SIZE));
        } catch (RuntimeException exception) {
            // 出图只是展示增强，失败时端上仍可显示并手输码串
            log.warn("取餐码二维码生成失败: orderNo={}, pickupCode={}",
                    order.getOrderNo(), order.getPickupCode(), exception);
        }
        return vo;
    }

    @Override
    public OrderVO detailForAdmin(Long storeId, String orderNo) {
        return toVO(requireByStore(storeId, orderNo));
    }

    @Override
    public PageResult<OrderSummaryVO> pageForApp(Long memberId, Long storeId,
                                                 Long current, Long size, String status) {
        IPage<TradeOrder> page = tradeOrderMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<TradeOrder>()
                        .eq(TradeOrder::getMemberId, memberId)
                        .eq(TradeOrder::getStoreId, storeId)
                        .eq(StringUtils.hasText(status), TradeOrder::getStatus, status)
                        .orderByDesc(TradeOrder::getCreateTime));
        return toPageResult(page, memberId);
    }

    @Override
    public PageResult<OrderSummaryVO> pageForAdmin(Long storeId, Long current, Long size,
                                                   String status, String keyword) {
        IPage<TradeOrder> page = tradeOrderMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<TradeOrder>()
                        .eq(TradeOrder::getStoreId, storeId)
                        .eq(StringUtils.hasText(status), TradeOrder::getStatus, status)
                        .likeRight(StringUtils.hasText(keyword), TradeOrder::getOrderNo, keyword)
                        .orderByDesc(TradeOrder::getCreateTime));
        return toPageResult(page, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO pay(Long memberId, Long storeId, String orderNo, OrderPayDTO dto) {
        // M4：模拟支付已换成 PayGateway 预下单 + 支付成功回调入账，规则只保留在 PaymentService 一份。
        return paymentService.prepay(memberId, storeId, orderNo, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO cancel(Long memberId, Long storeId, String orderNo) {
        TradeOrder order = requireOwned(memberId, storeId, orderNo);
        OrderStatus from = OrderStatus.of(order.getStatus());
        if (!doCancel(order, from, OperatorType.MEMBER, memberId, "顾客主动取消")) {
            // 并发下取消已被其它路径处理（超时消息或重复点击），不能再报成功
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "订单状态已变更，请刷新后重试");
        }
        return toVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO transition(Long storeId, String orderNo, String toStatus,
                              String remark, OperatorType operatorType, Long operatorId) {
        TradeOrder order = requireByStore(storeId, orderNo);
        OrderStatus from = OrderStatus.of(order.getStatus());
        OrderStatus to = OrderStatus.of(toStatus);
        if (to == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "目标状态不合法：" + toStatus);
        }
        OrderStatusTransition.assertAllowed(from, to);
        LocalDateTime completedTime = to == OrderStatus.COMPLETED ? LocalDateTime.now() : null;
        if (!compareAndSetStatus(order, from, to, null, completedTime)) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "订单状态已变更，请刷新后重试");
        }
        writeLog(order, from, to, operatorType, operatorId,
                StringUtils.hasText(remark) ? remark : "状态推进");
        publishStatus(order, from, to);
        if (to == OrderStatus.COMPLETED) {
            // 订单完成单独发一条事件：积分入账与等级升级都需要「完成」语义，
            // 用 STATUS_CHANGED 让消费者按 toStatus 过滤会把两件事混进通知链路
            publishCompleted(order);
        }
        log.info("订单状态已推进: orderNo={}, from={}, to={}, operator={}",
                orderNo, from, to, operatorType);
        return toVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOnTimeout(String orderNo, String messageId) {
        TradeOrder order = findByOrderNo(orderNo);
        if (order == null || !OrderStatus.CREATED.name().equals(order.getStatus())) {
            log.info("超时取消跳过（订单不存在或已非待支付）: orderNo={}, messageId={}", orderNo, messageId);
            return false;
        }
        boolean cancelled = doCancel(order, OrderStatus.CREATED, OperatorType.SYSTEM, null,
                "待支付超时自动取消，messageId=" + messageId);
        if (cancelled) {
            log.info("订单超时已取消并回补库存: orderNo={}, messageId={}", orderNo, messageId);
        }
        return cancelled;
    }

    @Override
    public void resendTimeoutMessage(Long storeId, String orderNo, long delaySeconds) {
        TradeOrder order = requireByStore(storeId, orderNo);
        if (!OrderStatus.CREATED.name().equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "仅待支付订单可补发超时消息");
        }
        long bounded = Math.max(1L, Math.min(delaySeconds, MAX_REPLAY_DELAY_SECONDS));
        publishTimeout(order, TimeUnit.SECONDS.toMillis(bounded));
        log.info("已补发短延迟超时消息: orderNo={}, delaySeconds={}", orderNo, bounded);
    }

    // ------------------------------------------------------------------ 私有逻辑

    /**
     * 组装并校验下单上下文：购物车归属、M2 金额与可售性、预定菜硬校验、订单类型与时间。
     */
    private Selection prepare(Long memberId, Long storeId, OrderCreateDTO dto) {
        List<CartItem> cartItems = loadCartItems(memberId, storeId, dto.getItemIds());
        OrderCheckDTO checkDTO = new OrderCheckDTO();
        checkDTO.setStoreId(storeId);
        // 会员价与「仅会员可购」判定在 DishOrderService 里，memberId 由服务端写入而不是前端传
        checkDTO.setMemberId(memberId);
        List<OrderCheckDTO.ItemDTO> rows = new ArrayList<>();
        for (CartItem item : cartItems) {
            OrderCheckDTO.ItemDTO row = new OrderCheckDTO.ItemDTO();
            row.setDishId(item.getDishId());
            row.setQuantity(item.getQuantity());
            row.setSpecOptionIds(CartSkuKeys.parse(item.getSpecKey()));
            row.setAddonIds(CartSkuKeys.parse(item.getAddonKeys()));
            rows.add(row);
        }
        checkDTO.setItems(rows);
        OrderCheckVO check = dishOrderService.check(checkDTO);
        if (BigDecimal.ZERO.compareTo(check.getTotalAmount()) >= 0) {
            throw new BizException(ErrorCode.CART_EMPTY, "订单金额必须大于 0");
        }
        Map<Long, Dish> dishMap = loadDishes(cartItems);
        boolean containsPreorder = dishMap.values().stream().anyMatch(DishConverter::isPreorder);
        int leadHours = DishConstants.PREORDER_HOURS;
        if (containsPreorder) {
            for (Dish dish : dishMap.values()) {
                if (DishConverter.isPreorder(dish) && dish.getPreorderHours() != null
                        && dish.getPreorderHours() > 0) {
                    leadHours = Math.max(leadHours, dish.getPreorderHours());
                }
            }
        }
        Selection selection = new Selection();
        selection.cartItems = cartItems;
        selection.check = check;
        selection.imageMap = loadImages(cartItems);
        selection.quantityByDish = aggregate(cartItems);
        selection.containsPreorder = containsPreorder;
        selection.preorderLeadHours = leadHours;
        resolveOrderRules(selection, dto);
        selection.dishAmounts = sumDishAmounts(check);
        if (dto.getUserCouponId() != null) {
            // 券的门槛、适用范围与抵扣额全部在服务端算，页面只回传选了哪张券
            selection.coupon = userCouponService.resolveForOrder(memberId, storeId,
                    dto.getUserCouponId(), check.getTotalAmount(), selection.dishAmounts);
        }
        // 会员折扣按商品总额算，独立于券；与不可叠加的券同单时「券优先」，折扣不生效而不是报错
        selection.memberDiscountAmount = MemberDiscountCalculator.amountOf(check.getTotalAmount(),
                pointsLevelService.discountRateOf(memberId));
        resolveStackConflict(selection);
        // 积分抵现排在券与会员折扣之后：先算券后并打折的金额，再按兑换比例算积分抵扣额
        BigDecimal afterDiscount = check.getTotalAmount().subtract(discountOf(selection))
                .subtract(selection.memberDiscountAmount);
        selection.pointsUsed = pointsAccountService.resolveUsePoints(memberId, dto.getUsePoints(),
                afterDiscount);
        selection.pointsAmount = pointsAccountService.amountOf(selection.pointsUsed);
        // 余额抵扣放在最后一段：只能抵掉前面三段之后剩下的应付额，且至少留 0.01 元给在线支付
        selection.balanceDeductAmount = resolveBalanceDeduct(memberId, dto.getUseBalance(),
                afterDiscount.subtract(selection.pointsAmount));
        return selection;
    }

    /**
     * 菜品 ID → 行金额小计，供单品券判定适用范围与抵扣基数。
     */
    private static Map<Long, BigDecimal> sumDishAmounts(OrderCheckVO check) {
        Map<Long, BigDecimal> result = new HashMap<>(8);
        if (check.getItems() == null) {
            return result;
        }
        for (OrderCheckVO.ItemVO item : check.getItems()) {
            if (item.getDishId() == null || item.getAmount() == null) {
                continue;
            }
            result.merge(item.getDishId(), item.getAmount(), BigDecimal::add);
        }
        return result;
    }

    private static BigDecimal discountOf(Selection selection) {
        return selection.coupon == null ? BigDecimal.ZERO : selection.coupon.getDiscountAmount();
    }

    /**
     * 券与会员折扣的叠加规则：{@code coupon_template.stackable = 1} 才允许两者同时生效；
     * 默认 0（不可同享）时**券优先、会员折扣置零**，不抛业务错误。
     *
     * <p>为什么不是报错：卡包里的券不按等级折扣过滤，选了券再被 30075 拦下会让顾客在任何折扣等级上
     * 都用不了历史券（M5 建模板时 stackable 一律落 0）。「券优先」既保住券的可用性，
     * 也让 stackable 成为「能否叠加」的开关，页面从 preview 的 memberDiscountAmount=0 就能看出结果。</p>
     */
    private static void resolveStackConflict(Selection selection) {
        if (selection.coupon == null || selection.memberDiscountAmount.signum() <= 0) {
            return;
        }
        Integer stackable = selection.coupon.getStackable();
        if (stackable == null || stackable != 1) {
            selection.memberDiscountAmount = BigDecimal.ZERO.setScale(2);
        }
    }

    /**
     * 余额抵扣额：{@code min(可用余额, 剩余应付 − 0.01)}，剩余应付不足 0.01 元时为 0。
     * 页面的「使用余额」只是开关，金额全部由服务端算，避免前端传一个夸大金额再来回退。
     */
    private BigDecimal resolveBalanceDeduct(Long memberId, Boolean useBalance, BigDecimal remaining) {
        if (!Boolean.TRUE.equals(useBalance) || remaining == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal limit = remaining.subtract(MIN_ONLINE_PAY_AMOUNT);
        if (limit.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal available = balanceService.availableOf(memberId);
        if (available.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return OrderAmountCalculator.scale(available.min(limit));
    }

    /**
     * M3 预定菜硬校验：提前 24 小时、强制 DINE_IN、不可选自提时段。
     */
    private void resolveOrderRules(Selection selection, OrderCreateDTO dto) {
        LocalDateTime now = LocalDateTime.now();
        if (selection.containsPreorder) {
            if (dto.getPickupTime() != null) {
                throw new BizException(ErrorCode.DISH_PREORDER_ONLY_DINE_IN,
                        "预定菜仅限到店用餐，不能自选提时段");
            }
            if (dto.getDineInTime() == null) {
                throw new BizException(ErrorCode.ORDER_DINE_IN_TIME_REQUIRED);
            }
            LocalDateTime earliest = now.plusHours(selection.preorderLeadHours);
            if (dto.getDineInTime().isBefore(earliest)) {
                throw new BizException(ErrorCode.ORDER_PREORDER_LEAD_TIME,
                        "预定菜需提前 " + selection.preorderLeadHours + " 小时，最早可选 "
                                + TIME_FORMATTER.format(earliest));
            }
            selection.orderType = OrderType.DINE_IN;
            selection.forcedDineIn = true;
            selection.dineInTime = dto.getDineInTime();
            return;
        }
        OrderType requested = OrderType.of(dto.getOrderType());
        selection.orderType = requested == null ? OrderType.PICKUP : requested;
        selection.forcedDineIn = false;
        if (selection.orderType == OrderType.PICKUP) {
            if (dto.getPickupTime() != null && dto.getPickupTime().isBefore(now)) {
                throw new BizException(ErrorCode.ORDER_PICKUP_TIME_INVALID, "自提时间不能早于当前时间");
            }
            selection.pickupTime = dto.getPickupTime();
            return;
        }
        if (dto.getDineInTime() == null) {
            throw new BizException(ErrorCode.ORDER_DINE_IN_TIME_REQUIRED);
        }
        if (dto.getDineInTime().isBefore(now)) {
            throw new BizException(ErrorCode.ORDER_PICKUP_TIME_INVALID, "到店时间不能早于当前时间");
        }
        selection.dineInTime = dto.getDineInTime();
    }

    private List<CartItem> loadCartItems(Long memberId, Long storeId, List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new BizException(ErrorCode.CART_EMPTY);
        }
        List<CartItem> items = cartItemMapper.selectBatchIds(itemIds);
        List<CartItem> owned = items.stream()
                .filter(item -> memberId.equals(item.getMemberId()) && storeId.equals(item.getStoreId()))
                .collect(Collectors.toList());
        if (owned.size() != itemIds.size()) {
            throw new BizException(ErrorCode.CART_ITEM_NOT_FOUND, "部分购物车行不存在或不属于当前会员");
        }
        return owned;
    }

    private Map<Long, Dish> loadDishes(List<CartItem> items) {
        List<Long> ids = items.stream().map(CartItem::getDishId).distinct().collect(Collectors.toList());
        return dishMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Dish::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, String> loadImages(List<CartItem> items) {
        List<Long> ids = items.stream().map(CartItem::getDishId).distinct().collect(Collectors.toList());
        List<DishImage> images = dishImageMapper.selectList(new LambdaQueryWrapper<DishImage>()
                .in(DishImage::getDishId, ids));
        Map<Long, String> result = new HashMap<>();
        for (DishImage image : images) {
            if (IMAGE_TYPE_LIST.equals(image.getImageType())) {
                result.putIfAbsent(image.getDishId(), image.getObjectKey());
            }
        }
        for (DishImage image : images) {
            if (IMAGE_TYPE_THUMB.equals(image.getImageType())) {
                result.putIfAbsent(image.getDishId(), image.getObjectKey());
            }
        }
        return result;
    }

    private static Map<Long, Integer> aggregate(List<CartItem> items) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (CartItem item : items) {
            result.merge(item.getDishId(), item.getQuantity(), Integer::sum);
        }
        return result;
    }

    /**
     * Redis Lua 预扣。key 缺失时按 DB remain 重建；整体失败时回补已扣的 key。
     */
    private List<Deduction> preDeduct(Long storeId, Map<Long, Integer> quantityByDish) {
        LocalDate today = LocalDate.now();
        List<Deduction> result = new ArrayList<>();
        try {
            for (Map.Entry<Long, Integer> entry : quantityByDish.entrySet()) {
                DishStock stock = loadStock(storeId, entry.getKey(), today);
                if (stock == null) {
                    // 未配置当日库存表示不限量，与 M2 assertOrderable 的口径一致
                    continue;
                }
                String key = StockKeys.dish(storeId, entry.getKey(), today);
                if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
                    stockService.reset(key, stock.getRemain());
                }
                if (!stockService.tryDeduct(key, entry.getValue())) {
                    throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, "库存不足，请调整份数后重试");
                }
                result.add(new Deduction(key, entry.getKey(), entry.getValue(), today));
            }
        } catch (RuntimeException e) {
            // Lua 执行异常（脚本返回 null/-1、Redis 抖动）也必须回补已扣的 key
            rollbackRedis(result);
            throw e;
        }
        return result;
    }

    private void registerRollbackCompensation(List<Deduction> deductions) {
        if (deductions.isEmpty() || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK || status == STATUS_UNKNOWN) {
                    rollbackRedis(deductions);
                }
            }
        });
    }

    private void rollbackRedis(List<Deduction> deductions) {
        for (Deduction deduction : deductions) {
            try {
                stockService.rollback(deduction.key, deduction.quantity, false);
            } catch (RuntimeException e) {
                log.error("库存 Redis 回补失败，等待每日重建兜底: key={}", deduction.key, e);
            }
        }
    }

    /**
     * 桌台只对堂食单生效：自提单即使带了 tableId 也丢弃，避免「自提单挂桌台」这种脏数据。
     * 桌台必须存在且属于当前门店，否则抛 30091。
     */
    private Long resolveTableId(Long storeId, OrderType orderType, Long tableId) {
        if (tableId == null || orderType != OrderType.DINE_IN) {
            return null;
        }
        StoreTable table = storeTableMapper.selectById(tableId);
        if (table == null || !storeId.equals(table.getStoreId())) {
            throw new BizException(ErrorCode.STORE_TABLE_NOT_FOUND);
        }
        return tableId;
    }

    private String generateCode(Long storeId, LocalDate bizDate, OrderType type) {
        String sequenceKey = type == OrderType.DINE_IN
                ? OrderKeys.dineInSequence(storeId, bizDate) : OrderKeys.pickupSequence(storeId, bizDate);
        for (int attempt = 0; attempt < MAX_CODE_RETRY; attempt++) {
            Long sequence = stringRedisTemplate.opsForValue().increment(sequenceKey);
            if (sequence == null) {
                throw new BizException(ErrorCode.SYSTEM_ERROR, "取餐码序号生成失败");
            }
            stringRedisTemplate.expire(sequenceKey, CODE_KEY_TTL_HOURS, TimeUnit.HOURS);
            String code = type == OrderType.DINE_IN
                    ? PickupCodeGenerator.dineIn(sequence) : PickupCodeGenerator.pickup(sequence);
            Long exists = tradeOrderMapper.countByPickupCode(storeId, bizDate, code);
            if (exists == null || exists == 0L) {
                return code;
            }
            log.warn("取餐码撞已有记录，换下一个序号: code={}", code);
        }
        throw new BizException(ErrorCode.SYSTEM_ERROR, "取餐码生成冲突次数过多");
    }

    private void insertItems(TradeOrder order, Selection selection) {
        List<OrderCheckVO.ItemVO> rows = selection.check.getItems();
        for (int index = 0; index < selection.cartItems.size(); index++) {
            CartItem cart = selection.cartItems.get(index);
            OrderCheckVO.ItemVO row = rows.get(index);
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setDishId(cart.getDishId());
            item.setDishName(row.getDishName());
            item.setSpecDesc(describeOptions(CartSkuKeys.parse(cart.getSpecKey())));
            item.setAddonDesc(describeAddons(CartSkuKeys.parse(cart.getAddonKeys())));
            item.setPrice(row.getUnitPrice());
            item.setQuantity(row.getQuantity());
            item.setAmount(row.getAmount());
            item.setObjectKey(selection.imageMap.getOrDefault(cart.getDishId(), ""));
            orderItemMapper.insert(item);
        }
    }

    /**
     * 兑换单只有一行（一道菜），规格 / 加料的展示文案按提交的 ID 反查，与普通下单同口径。
     */
    private void insertExchangeItems(TradeOrder order, OrderCheckVO check,
                                     List<Long> specOptionIds, List<Long> addonIds, String objectKey) {
        String specDesc = describeOptions(specOptionIds == null
                ? Collections.emptyList() : specOptionIds);
        String addonDesc = describeAddons(addonIds == null ? Collections.emptyList() : addonIds);
        for (OrderCheckVO.ItemVO row : check.getItems()) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setDishId(row.getDishId());
            item.setDishName(row.getDishName());
            item.setSpecDesc(specDesc);
            item.setAddonDesc(addonDesc);
            item.setPrice(row.getUnitPrice());
            item.setQuantity(row.getQuantity());
            item.setAmount(row.getAmount());
            item.setObjectKey(objectKey == null ? "" : objectKey);
            orderItemMapper.insert(item);
        }
    }

    /**
     * 菜品列表图：优先 LIST，其次 THUMB，都没有返回空串（与下单快照同口径）。
     */
    private String loadImageOf(Long dishId) {
        List<DishImage> images = dishImageMapper.selectList(new LambdaQueryWrapper<DishImage>()
                .eq(DishImage::getDishId, dishId));
        for (DishImage image : images) {
            if (IMAGE_TYPE_LIST.equals(image.getImageType())) {
                return image.getObjectKey();
            }
        }
        for (DishImage image : images) {
            if (IMAGE_TYPE_THUMB.equals(image.getImageType())) {
                return image.getObjectKey();
            }
        }
        return "";
    }

    private void writeLog(TradeOrder order, OrderStatus from, OrderStatus to,
                          OperatorType operatorType, Long operatorId, String remark) {
        OrderStatusLog logRecord = new OrderStatusLog();
        logRecord.setOrderId(order.getId());
        logRecord.setFromStatus(from == null ? "" : from.name());
        logRecord.setToStatus(to.name());
        logRecord.setOperatorType(operatorType.name());
        logRecord.setOperatorId(operatorId);
        logRecord.setRemark(remark);
        orderStatusLogMapper.insert(logRecord);
    }

    private boolean doCancel(TradeOrder order, OrderStatus from, OperatorType operatorType,
                             Long operatorId, String remark) {
        OrderStatusTransition.assertAllowed(from, OrderStatus.CANCELLED);
        // 条件更新是并发幂等的唯一判据：顾客取消与超时消息同时到达时只有一个能成功，
        // 失败方直接返回，不会二次回补库存
        if (!compareAndSetStatus(order, from, OrderStatus.CANCELLED, null, null)) {
            return false;
        }
        writeLog(order, from, OrderStatus.CANCELLED, operatorType, operatorId, remark);
        List<Restore> restores = restoreDbStock(order);
        scheduleRedisRestore(restores);
        // 取消即退回券：只退回被本订单锁定的那张，已核销的券不会被误退
        userCouponService.releaseForOrder(order.getCouponId(), order.getId());
        // 积分抵现同样要退回：只回滚仍处于冻结状态的流水，已转实扣的订单不可取消
        pointsAccountService.rollbackForOrder(order.getMemberId(), order.getOrderNo());
        // 储值余额抵扣退回：幂等键 {orderNo}#ROLLBACK，重复取消不会重复加钱
        balanceService.rollbackForOrder(order.getMemberId(), order.getOrderNo());
        // 积分兑换单退回积分：普通订单没有 EXCHANGE 流水，这里直接 no-op
        pointsAccountService.rollbackExchange(order.getMemberId(), order.getOrderNo());
        publishStatus(order, from, OrderStatus.CANCELLED);
        return true;
    }

    /**
     * 带旧状态条件的更新，防止并发下丢失更新与重复发状态消息。
     */
    private boolean compareAndSetStatus(TradeOrder order, OrderStatus from, OrderStatus to,
                                        LocalDateTime paidTime, LocalDateTime completedTime) {
        LambdaUpdateWrapper<TradeOrder> update = new LambdaUpdateWrapper<TradeOrder>()
                .eq(TradeOrder::getId, order.getId())
                .eq(TradeOrder::getStatus, from.name())
                .set(TradeOrder::getStatus, to.name());
        if (paidTime != null) {
            update.set(TradeOrder::getPaidTime, paidTime);
        }
        if (completedTime != null) {
            update.set(TradeOrder::getCompletedTime, completedTime);
        }
        if (tradeOrderMapper.update(null, update) == 0) {
            return false;
        }
        order.setStatus(to.name());
        if (paidTime != null) {
            order.setPaidTime(paidTime);
        }
        if (completedTime != null) {
            order.setCompletedTime(completedTime);
        }
        return true;
    }

    /**
     * 取消时回补 DB 余量，记录需要同步回补 Redis 的 key 与份数。
     */
    private List<Restore> restoreDbStock(TradeOrder order) {
        List<OrderItem> items = loadItems(order.getId());
        Map<Long, Integer> quantityByDish = new LinkedHashMap<>();
        for (OrderItem item : items) {
            quantityByDish.merge(item.getDishId(), item.getQuantity(), Integer::sum);
        }
        LocalDate date = order.getBizDate() == null ? LocalDate.now() : order.getBizDate();
        List<Restore> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantityByDish.entrySet()) {
            if (dishStockMapper.restore(order.getStoreId(), entry.getKey(), date, entry.getValue()) > 0) {
                result.add(new Restore(StockKeys.dish(order.getStoreId(), entry.getKey(), date),
                        order.getStoreId(), entry.getKey(), date, entry.getValue()));
            }
        }
        return result;
    }

    /**
     * 事务提交后增量回补 Redis；不整体 SET，避免覆盖同一时刻其他订单的预扣。
     */
    private void scheduleRedisRestore(List<Restore> restores) {
        if (restores.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            applyRedisRestore(restores);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                applyRedisRestore(restores);
            }
        });
    }

    private void applyRedisRestore(List<Restore> restores) {
        for (Restore restore : restores) {
            try {
                if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(restore.key))) {
                    // key 已过期 / Redis 重启：按 DB 真实值重建，不从 0 开始累加
                    DishStock stock = loadStock(restore.storeId, restore.dishId, restore.date);
                    if (stock != null) {
                        stockService.reset(restore.key, stock.getRemain());
                    }
                    continue;
                }
                stockService.rollback(restore.key, restore.quantity, false);
            } catch (RuntimeException e) {
                log.error("库存 Redis 回补失败，等待每日重建兜底: key={}", restore.key, e);
            }
        }
    }

    private void publishStatus(TradeOrder order, OrderStatus from, OrderStatus to) {
        OrderEventMessage message = new OrderEventMessage();
        message.setMessageId(newMessageId());
        message.setEventType(OrderEventMessage.TYPE_STATUS_CHANGED);
        message.setOrderNo(order.getOrderNo());
        message.setOrderId(order.getId());
        message.setMemberId(order.getMemberId());
        message.setStoreId(order.getStoreId());
        message.setFromStatus(from == null ? "" : from.name());
        message.setToStatus(to.name());
        message.setOccurredAtMillis(System.currentTimeMillis());
        messageProducer.publish(envelope(RabbitConst.ORDER_EXCHANGE, RabbitConst.RK_ORDER_NOTIFY,
                0L, message));
    }

    /**
     * 订单完成事件：走 member 交换机，供 M6 的积分入账与等级升级消费。
     */
    private void publishCompleted(TradeOrder order) {
        OrderEventMessage message = new OrderEventMessage();
        message.setMessageId(newMessageId());
        message.setEventType(OrderEventMessage.TYPE_ORDER_COMPLETED);
        message.setOrderNo(order.getOrderNo());
        message.setOrderId(order.getId());
        message.setMemberId(order.getMemberId());
        message.setStoreId(order.getStoreId());
        message.setFromStatus(OrderStatus.READY.name());
        message.setToStatus(OrderStatus.COMPLETED.name());
        message.setOccurredAtMillis(System.currentTimeMillis());
        messageProducer.publish(envelope(RabbitConst.MEMBER_EXCHANGE, RabbitConst.RK_POINTS_COMPLETED,
                0L, message));
    }

    private void publishTimeout(TradeOrder order, long delayMillis) {
        OrderEventMessage message = new OrderEventMessage();
        message.setMessageId(newMessageId());
        message.setEventType(OrderEventMessage.TYPE_TIMEOUT_CANCEL);
        message.setOrderNo(order.getOrderNo());
        message.setOrderId(order.getId());
        message.setMemberId(order.getMemberId());
        message.setStoreId(order.getStoreId());
        message.setToStatus(OrderStatus.CANCELLED.name());
        message.setOccurredAtMillis(System.currentTimeMillis());
        messageProducer.publish(envelope(RabbitConst.DELAY_EXCHANGE, RabbitConst.RK_ORDER_TIMEOUT,
                delayMillis, message));
    }

    private static MqEnvelope envelope(String exchange, String routingKey,
                                       long delayMillis, OrderEventMessage message) {
        MqEnvelope result = new MqEnvelope();
        result.setMessageId(message.getMessageId());
        result.setExchangeName(exchange);
        result.setRoutingKey(routingKey);
        result.setDelayMillis(delayMillis);
        result.setBody(message);
        return result;
    }

    private static String newMessageId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String generateOrderNo() {
        return "CO" + LocalDateTime.now().format(ORDER_NO_FORMATTER)
                + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private String describeOptions(List<Long> ids) {
        if (ids.isEmpty()) {
            return "";
        }
        return dishSpecOptionMapper.selectBatchIds(ids).stream()
                .map(DishSpecOption::getOptionName).collect(Collectors.joining("、"));
    }

    private String describeAddons(List<Long> ids) {
        if (ids.isEmpty()) {
            return "";
        }
        return dishAddonMapper.selectBatchIds(ids).stream()
                .map(DishAddon::getAddonName).collect(Collectors.joining("、"));
    }

    private DishStock loadStock(Long storeId, Long dishId, LocalDate date) {
        return dishStockMapper.selectOne(new LambdaQueryWrapper<DishStock>()
                .eq(DishStock::getStoreId, storeId)
                .eq(DishStock::getDishId, dishId)
                .eq(DishStock::getStockDate, date));
    }

    private List<OrderItem> loadItems(Long orderId) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getCreateTime));
    }

    private List<OrderStatusLog> loadLogs(Long orderId) {
        return orderStatusLogMapper.selectList(new LambdaQueryWrapper<OrderStatusLog>()
                .eq(OrderStatusLog::getOrderId, orderId)
                .orderByAsc(OrderStatusLog::getCreateTime));
    }

    private OrderVO toVO(TradeOrder order) {
        return OrderConverter.toVO(order, loadItems(order.getId()), loadLogs(order.getId()));
    }

    private PageResult<OrderSummaryVO> toPageResult(IPage<TradeOrder> page, Long memberId) {
        List<TradeOrder> orders = page.getRecords();
        Map<Long, List<OrderItem>> itemsByOrder = Collections.emptyMap();
        if (!orders.isEmpty()) {
            List<Long> ids = orders.stream().map(TradeOrder::getId).collect(Collectors.toList());
            itemsByOrder = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                            .in(OrderItem::getOrderId, ids))
                    .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
        }
        // 顾客端才需要评价入口；后台列表不查评价表，避免多一次无意义的聚合查询
        Set<Long> reviewedOrderIds = memberId == null ? Collections.emptySet()
                : reviewService.reviewedOrderIds(memberId,
                orders.stream().map(TradeOrder::getId).collect(Collectors.toList()));
        Map<Long, List<OrderItem>> finalItems = itemsByOrder;
        List<OrderSummaryVO> records = orders.stream()
                .map(order -> {
                    OrderSummaryVO vo = OrderConverter.toSummaryVO(order,
                            finalItems.getOrDefault(order.getId(), Collections.emptyList()));
                    boolean reviewed = reviewedOrderIds.contains(order.getId());
                    vo.setReviewed(reviewed);
                    vo.setCanReview(OrderStatus.COMPLETED.name().equals(order.getStatus()) && !reviewed);
                    return vo;
                })
                .collect(Collectors.toList());
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    private TradeOrder findByOrderNo(String orderNo) {
        return tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrder>()
                .eq(TradeOrder::getOrderNo, orderNo));
    }

    private TradeOrder requireOwned(Long memberId, Long storeId, String orderNo) {
        TradeOrder order = findByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (!memberId.equals(order.getMemberId()) || !storeId.equals(order.getStoreId())) {
            throw new BizException(ErrorCode.ORDER_ACCESS_DENIED);
        }
        return order;
    }

    private TradeOrder requireByStore(Long storeId, String orderNo) {
        TradeOrder order = findByOrderNo(orderNo);
        if (order == null || !storeId.equals(order.getStoreId())) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /**
     * 下单上下文，纯内部数据载体，不外泄到接口层。
     */
    private static final class Selection {

        private List<CartItem> cartItems;

        private OrderCheckVO check;

        private Map<Long, String> imageMap;

        private Map<Long, Integer> quantityByDish;

        private boolean containsPreorder;

        private int preorderLeadHours;

        private OrderType orderType;

        private boolean forcedDineIn;

        private LocalDateTime pickupTime;

        private LocalDateTime dineInTime;

        private Map<Long, BigDecimal> dishAmounts;

        private CouponSelectionVO coupon;

        private int pointsUsed;

        private BigDecimal pointsAmount = BigDecimal.ZERO;

        /** 会员折扣，按商品总额 × (1 − 等级折扣率) 由服务端算。 */
        private BigDecimal memberDiscountAmount = BigDecimal.ZERO;

        /** 储值余额抵扣，按可用余额与剩余应付额由服务端算。 */
        private BigDecimal balanceDeductAmount = BigDecimal.ZERO;
    }

    private static final class Deduction {

        private final String key;

        private final Long dishId;

        private final Integer quantity;

        private final LocalDate date;

        private Deduction(String key, Long dishId, Integer quantity, LocalDate date) {
            this.key = key;
            this.dishId = dishId;
            this.quantity = quantity;
            this.date = date;
        }
    }

    private static final class Restore {

        private final String key;

        private final Long storeId;

        private final Long dishId;

        private final LocalDate date;

        private final Integer quantity;

        private Restore(String key, Long storeId, Long dishId, LocalDate date, Integer quantity) {
            this.key = key;
            this.storeId = storeId;
            this.dishId = dishId;
            this.date = date;
            this.quantity = quantity;
        }
    }
}
