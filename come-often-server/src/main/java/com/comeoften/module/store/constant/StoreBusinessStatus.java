package com.comeoften.module.store.constant;

/**
 * 门店营业状态。DDL 注释即 0-休息中 / 1-营业中，B2 之前这两个值散落在转换器与前端里，
 * 现在收口到一处，下单 / 取号 / 领券的前置校验统一引用这里，避免各处再写字面量。
 */
public final class StoreBusinessStatus {

    /**
     * 休息中：不受理新订单、新排队号与新券领取。
     */
    public static final int CLOSED = 0;

    /**
     * 营业中。
     */
    public static final int OPEN = 1;

    private StoreBusinessStatus() {
    }

    /**
     * 是否营业中。null 按休息中处理，与 StoreConverter 的下发文案口径一致。
     */
    public static boolean isOpen(Integer status) {
        return status != null && status == OPEN;
    }
}
