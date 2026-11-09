package com.comeoften.module.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情。金额全部为下单时快照，禁止事后重算；雪花 ID 一律序列化成字符串。
 */
@Data
@Schema(description = "订单详情")
public class OrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "订单 ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单类型：PICKUP/DINE_IN")
    private String orderType;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "状态中文说明")
    private String statusText;

    @Schema(description = "商品总额，单位元")
    private BigDecimal totalAmount;

    @Schema(description = "优惠抵扣，单位元")
    private BigDecimal discountAmount;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "使用的用户券 ID")
    private Long couponId;

    @Schema(description = "积分抵现，单位元")
    private BigDecimal pointsAmount;

    @Schema(description = "本单使用的积分数量")
    private Integer pointsUsed;

    @Schema(description = "定金，单位元；整单内抵扣，不额外加收")
    private BigDecimal depositAmount;

    @Schema(description = "实付金额，单位元")
    private BigDecimal payAmount;

    @Schema(description = "到店待付尾款，单位元")
    private BigDecimal balanceAmount;

    @Schema(description = "是否含预定菜")
    private Boolean containsPreorder;

    @Schema(description = "是否被服务端强制为 DINE_IN")
    private Boolean forcedDineIn;

    @Schema(description = "取餐码；DINE_IN 单为到店核销码")
    private String pickupCode;

    @Schema(description = "取餐码归属营业日")
    private LocalDate bizDate;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "预约自提时间")
    private LocalDateTime pickupTime;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "到店用餐时间")
    private LocalDateTime dineInTime;

    @Schema(description = "备注")
    private String remark;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "支付时间")
    private LocalDateTime paidTime;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "完成时间")
    private LocalDateTime completedTime;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "下单时间")
    private LocalDateTime createTime;

    @Schema(description = "定金说明")
    private String depositNote;

    @Schema(description = "是否可评价：订单已完成且尚未评价，由服务端判定")
    private Boolean canReview;

    @Schema(description = "是否已评价")
    private Boolean reviewed;

    @Schema(description = "订单明细快照")
    private List<ItemVO> items;

    @Schema(description = "状态日志")
    private List<LogVO> logs;

    /**
     * 订单明细。
     */
    @Data
    @Schema(description = "订单明细快照")
    public static class ItemVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "菜品 ID")
        private Long dishId;

        @Schema(description = "菜名快照")
        private String dishName;

        @Schema(description = "规格快照")
        private String specDesc;

        @Schema(description = "加料快照")
        private String addonDesc;

        @Schema(description = "单价快照，单位元")
        private BigDecimal price;

        @Schema(description = "份数")
        private Integer quantity;

        @Schema(description = "行金额快照，单位元")
        private BigDecimal amount;

        @Schema(description = "菜品图 objectKey 快照")
        private String objectKey;
    }

    /**
     * 状态日志。
     */
    @Data
    @Schema(description = "订单状态日志")
    public static class LogVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "迁移前状态")
        private String fromStatus;

        @Schema(description = "迁移后状态")
        private String toStatus;

        @Schema(description = "操作方")
        private String operatorType;

        @Schema(description = "说明")
        private String remark;

        @JsonFormat(pattern = TIME_PATTERN)
        @Schema(description = "发生时间")
        private LocalDateTime createTime;
    }
}
