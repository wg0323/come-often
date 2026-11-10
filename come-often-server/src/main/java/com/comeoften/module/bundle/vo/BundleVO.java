package com.comeoften.module.bundle.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 团购套餐展示。顾客端与后台共用，金额单位元。
 */
@Data
@Schema(description = "团购套餐")
public class BundleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "套餐 ID")
    private Long id;

    @Schema(description = "套餐名称")
    private String bundleName;

    @Schema(description = "套餐说明")
    private String description;

    @Schema(description = "套餐售价，单位元")
    private BigDecimal bundlePrice;

    @Schema(description = "划线价，单位元")
    private BigDecimal originalPrice;

    @Schema(description = "套餐总库存")
    private Integer totalStock;

    @Schema(description = "剩余库存")
    private Integer remainStock;

    @Schema(description = "状态：0-下架，1-上架")
    private Integer status;

    @Schema(description = "状态文案")
    private String statusText;

    @Schema(description = "套餐明细")
    private List<BundleItemVO> items;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
