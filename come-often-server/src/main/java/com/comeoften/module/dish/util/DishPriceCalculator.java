package com.comeoften.module.dish.util;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * 菜品金额计算。页面不做业务计算，规格与加料的加价一律由服务端汇总后返回。
 * 单位价 = 基础价 + 各规格选项加价 + 各加料加价；总价 = 单位价 × 数量。
 */
public final class DishPriceCalculator {

    private static final int SCALE = 2;

    private DishPriceCalculator() {
    }

    /**
     * 计算单品行金额，结果保留两位小数（HALF_UP）。
     *
     * @param basePrice   菜品基础售价
     * @param specDeltas  已选规格选项的加价集合，可为 null
     * @param addonDeltas 已选加料的加价集合，可为 null
     * @param quantity    份数，必须大于 0
     * @return 行金额
     */
    public static BigDecimal quote(BigDecimal basePrice, Collection<BigDecimal> specDeltas,
                                   Collection<BigDecimal> addonDeltas, int quantity) {
        if (basePrice == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "菜品基础价不能为空");
        }
        if (quantity <= 0) {
            throw new BizException(ErrorCode.PARAM_INVALID, "菜品份数必须大于 0");
        }
        BigDecimal unitPrice = basePrice;
        unitPrice = addAll(unitPrice, specDeltas);
        unitPrice = addAll(unitPrice, addonDeltas);
        // 先把单位价规范到分，保证「展示的单位价 × 份数 = 行金额」自洽
        unitPrice = unitPrice.setScale(SCALE, RoundingMode.HALF_UP);
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal addAll(BigDecimal current, Collection<BigDecimal> deltas) {
        if (deltas == null || deltas.isEmpty()) {
            return current;
        }
        BigDecimal result = current;
        for (BigDecimal delta : deltas) {
            result = result.add(delta == null ? BigDecimal.ZERO : delta);
        }
        return result;
    }
}
