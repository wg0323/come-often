package com.comeoften.common;

/**
 * 业务错误码。分段：1xxxx 通用、2xxxx 商品与内容、3xxxx 库存与订单、4xxxx 鉴权（401xx 未登录 / 403xx 无权限）。
 * 401xx 与 403xx 由 GlobalExceptionHandler 映射为对应 HTTP 状态码，其余保持 200 + 业务码。
 */
public enum ErrorCode {

    SUCCESS(0, "success"),
    SYSTEM_ERROR(10000, "系统繁忙，请稍后再试"),
    PARAM_INVALID(10001, "参数不合法"),
    STORE_NOT_FOUND(20001, "门店不存在"),
    DISH_NOT_FOUND(20002, "菜品不存在"),
    DISH_CATEGORY_NOT_FOUND(20003, "菜品分类不存在"),
    DISH_OFF_SHELF(20004, "菜品已下架"),
    DISH_SOLD_OUT(20005, "菜品今日已售罄"),
    DISH_SPEC_INVALID(20006, "规格选项不合法"),
    DISH_ADDON_INVALID(20007, "加料不合法"),
    DISH_PREORDER_ONLY_DINE_IN(20008, "预定菜仅限到店用餐"),
    FILE_OBJECT_KEY_INVALID(20009, "对象键不合法"),
    DISH_CATEGORY_NAME_DUPLICATE(20010, "同门店下分类名称已存在"),
    UNAUTHORIZED(40100, "未登录"),
    TOKEN_EXPIRED(40101, "登录已过期"),
    TOKEN_INVALID(40102, "登录凭证非法"),
    TOKEN_CLIENT_MISMATCH(40103, "登录凭证与当前端不匹配"),
    LOGIN_FAILED(40104, "账号或密码错误"),
    ACCOUNT_DISABLED(40105, "账号已停用"),
    WX_CODE_INVALID(40106, "微信登录凭证无效"),
    WX_ALREADY_BOUND(40107, "该微信已绑定其他员工"),
    WX_PHONE_INVALID(40108, "微信手机号获取失败，请重新授权"),
    FORBIDDEN(40300, "无权限"),
    PERMISSION_DENIED(40301, "无操作权限"),
    STORE_UNBOUND(40302, "员工未绑定门店"),
    STOCK_NOT_ENOUGH(30001, "库存不足"),
    ORDER_STATUS_INVALID(30002, "订单状态流转非法"),
    CART_ITEM_NOT_FOUND(30003, "购物车项不存在"),
    CART_EMPTY(30004, "购物车为空"),
    ORDER_NOT_FOUND(30005, "订单不存在"),
    ORDER_ACCESS_DENIED(30006, "无权操作该订单"),
    ORDER_PREORDER_LEAD_TIME(30007, "预定菜需提前 24 小时预订"),
    ORDER_DINE_IN_TIME_REQUIRED(30008, "请选择到店用餐时间"),
    ORDER_PICKUP_TIME_INVALID(30009, "自提时间不合法"),
    ORDER_PAY_AMOUNT_INVALID(30010, "支付金额不合法"),
    MQ_MESSAGE_NOT_FOUND(30011, "本地消息不存在"),
    PAY_NOTIFY_INVALID(30012, "支付回调报文不合法"),
    PAY_SIGN_INVALID(30013, "支付回调验签失败"),
    PAY_GATEWAY_NOT_ENABLED(30014, "支付网关未启用"),
    PAYMENT_RECORD_NOT_FOUND(30015, "支付流水不存在"),
    REFUND_NOT_ALLOWED(30016, "当前订单状态不支持退款"),
    REFUND_RECORD_NOT_FOUND(30017, "退款单不存在"),
    REFUND_ALREADY_APPLIED(30018, "该订单已有处理中的退款申请"),
    REFUND_STATUS_INVALID(30019, "退款单状态不允许审核"),
    REFUND_AMOUNT_INVALID(30020, "退款金额不合法"),
    REFUND_DEPOSIT_MERCHANT_ONLY(30021, "定金订单请联系商家发起退款"),
    COUPON_TEMPLATE_NOT_FOUND(30022, "券模板不存在"),
    COUPON_TEMPLATE_NOT_ISSUING(30023, "券未发放或已停用"),
    COUPON_SOLD_OUT(30024, "券已领完"),
    COUPON_NOT_FOUND(30025, "券不存在"),
    COUPON_ALREADY_USED(30026, "券已使用"),
    COUPON_EXPIRED(30027, "券已过期"),
    COUPON_CODE_INVALID(30028, "券码已失效，请刷新券码后重试"),
    COUPON_THRESHOLD_NOT_ENOUGH(30029, "未达到券使用门槛"),
    COUPON_STORE_MISMATCH(30030, "该券不适用于当前门店"),
    COUPON_DISH_NOT_MATCHED(30031, "该券不适用于当前订单菜品"),
    COUPON_RECEIVE_CLOSED(30032, "券不在可领取时间内"),
    POINTS_BALANCE_NOT_ENOUGH(30033, "积分余额不足"),
    POINTS_USE_AMOUNT_INVALID(30034, "使用的积分不合法"),
    POINTS_ACCOUNT_NOT_FOUND(30035, "积分账户不存在"),
    POINTS_SIGN_IN_DUPLICATE(30036, "今日已签到"),
    POINTS_SHARE_DUPLICATE(30037, "今日分享积分已领取"),
    POINTS_PROFILE_COMPLETED(30038, "完善资料积分已领取"),
    MEMBER_LEVEL_NOT_FOUND(30039, "会员等级不存在"),
    MEMBER_LEVEL_STATUS_INVALID(30040, "等级门槛配置不合法"),
    RESERVATION_SLOT_FULL(30041, "该时段已满，请选择其他时段"),
    RESERVATION_SLOT_NOT_FOUND(30042, "预约时段不存在或未开放"),
    RESERVATION_NOT_FOUND(30043, "预约单不存在"),
    RESERVATION_PEOPLE_INVALID(30044, "就餐人数与所选桌型不匹配"),
    RESERVATION_CANCEL_TOO_LATE(30045, "距预约时间不足 2 小时，无法取消"),
    RESERVATION_STATUS_INVALID(30046, "当前预约状态不允许该操作"),
    RESERVATION_ACCESS_DENIED(30047, "无权操作该预约"),
    RESERVATION_CODE_INVALID(30048, "签到码已失效，请刷新后重试"),
    RESERVATION_TIME_INVALID(30049, "预约时间不合法"),
    RESERVATION_TABLE_TYPE_INVALID(30050, "该门店未开放所选桌型"),
    REVIEW_DUPLICATE(30051, "该订单已评价"),
    REVIEW_ORDER_NOT_COMPLETED(30052, "订单完成后才能评价"),
    REVIEW_DISH_INVALID(30053, "评价菜品不在该订单内"),
    REVIEW_IMAGE_INVALID(30054, "评价图片数量或对象键不合法"),
    STATS_DATE_RANGE_INVALID(30055, "统计区间不合法，最多查询 366 天"),
    STATS_EXPORT_ROW_LIMIT_EXCEEDED(30056, "导出数据超过行数上限，请缩小查询区间后重试"),
    QUEUE_TICKET_NOT_FOUND(30057, "排队号不存在"),
    QUEUE_TICKET_STATUS_INVALID(30058, "当前排队号状态不允许该操作"),
    QUEUE_TICKET_DUPLICATED(30059, "今日已有排队号，请勿重复取号"),
    QUEUE_PARTY_SIZE_INVALID(30062, "就餐人数不合法，请选择 1~20 位");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
