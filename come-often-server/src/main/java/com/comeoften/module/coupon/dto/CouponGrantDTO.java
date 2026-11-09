package com.comeoften.module.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 后台定向发券请求。发放同样走 StockService 预扣 + DB remain 兜底，不允许绕过库存。
 */
@Data
@Schema(description = "后台发券请求")
public class CouponGrantDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "券模板不能为空")
    @Schema(description = "券模板 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long templateId;

    @NotEmpty(message = "请选择发放会员")
    @Schema(description = "会员 ID 集合", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> memberIds;
}
