package com.comeoften.module.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 下单请求。金额不参与传参，服务端按购物车行重新试算；含预定菜时 orderType 被强制覆盖。
 */
@Data
@Schema(description = "下单请求")
public class OrderCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @NotEmpty(message = "请选择要结算的购物车商品")
    @Schema(description = "要结算的购物车行 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> itemIds;

    @Schema(description = "期望订单类型：PICKUP/DINE_IN；含预定菜时强制为 DINE_IN", example = "PICKUP")
    private String orderType;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "预约自提时间；含预定菜时不可传")
    private LocalDateTime pickupTime;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "到店用餐时间；含预定菜时必填且须早于当前时间 24 小时以上", example = "2026-11-10 12:00:00")
    private LocalDateTime dineInTime;

    @Size(max = 255, message = "备注最多 255 字")
    @Schema(description = "备注：少盐 / 加辣 / 打包")
    private String remark;

    @Schema(description = "使用的用户券 ID；一个订单最多一张券，不传表示不用券")
    private Long userCouponId;

    @Min(value = 0, message = "使用的积分不能为负")
    @Schema(description = "使用的积分数量；不传或 0 表示不抵现，抵扣金额由服务端按兑换比例算出", example = "500")
    private Integer usePoints;
}
