package com.comeoften.module.stats.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 内部聚合行：区间券 ROI 的原始计数与金额。grossPaid 与 refundAmount 在转换时合成净额。
 */
@Data
public class StatsCouponRoiRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long templateId;

    private String templateName;

    private String couponType;

    private Integer total;

    private Long receivedCount;

    private Long usedCount;

    private Long orderCount;

    private BigDecimal grossPaid;

    private BigDecimal discountAmount;

    private BigDecimal refundAmount;
}
