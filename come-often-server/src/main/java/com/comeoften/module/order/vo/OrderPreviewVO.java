package com.comeoften.module.order.vo;

import com.comeoften.module.coupon.vo.UserCouponVO;
import com.comeoften.module.dish.vo.OrderCheckVO;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 确认订单页数据。items 直接复用 /dish/order-check 返回的金额明细，页面不自行计算。
 */
@Data
@Schema(description = "确认订单页数据")
public class OrderPreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "金额明细，与 /dish/order-check 完全一致")
    private List<OrderCheckVO.ItemVO> items;

    @Schema(description = "商品总额，单位元")
    private BigDecimal totalAmount;

    @Schema(description = "券抵扣金额，单位元；未选券为 0")
    private BigDecimal discountAmount;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "已选用户券 ID")
    private Long couponId;

    @Schema(description = "已选券名称")
    private String couponName;

    @Schema(description = "积分抵现金额，单位元；未用积分为 0")
    private BigDecimal pointsAmount;

    @Schema(description = "本次实际使用的积分数量")
    private Integer pointsUsed;

    @Schema(description = "会员当前可用积分余额")
    private Integer pointsBalance;

    @Schema(description = "本单最多可用积分，已按券后金额留出 0.01 元计算")
    private Integer maxUsablePoints;

    @Schema(description = "积分兑换比例：多少积分抵 1 元")
    private Integer pointsExchangeRate;

    @Schema(description = "整单定金，单位元；含预定菜为 min(200, 券后实付金额)")
    private BigDecimal depositAmount;

    @Schema(description = "实付金额 = 商品总额 − 券抵扣 − 积分抵现；定金已包含在整单内，不额外加收")
    private BigDecimal payAmount;

    @Schema(description = "到店待付尾款，单位元")
    private BigDecimal balanceAmount;

    @Schema(description = "本次订单可用的券，抵扣额已由服务端算好，按抵扣额倒序")
    private List<UserCouponVO> availableCoupons;

    @Schema(description = "是否含预定菜")
    private Boolean containsPreorder;

    @Schema(description = "服务端判定后的订单类型")
    private String orderType;

    @Schema(description = "是否由服务端强制改为 DINE_IN")
    private Boolean forcedDineIn;

    @Schema(description = "需提前预订小时数，固定 24")
    private Integer preorderLeadHours;

    @Schema(description = "最早可选择的到店时间 yyyy-MM-dd HH:mm:ss；非预定单为空")
    private String earliestDineInTime;

    @Schema(description = "定金说明")
    private String depositNote;
}
