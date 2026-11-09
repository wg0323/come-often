package com.comeoften.module.reservation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 可预约桌型选项。人数区间是业务规则，桌数来自 store_table 聚合。
 */
@Data
@Schema(description = "可预约桌型选项")
public class ReservationTableTypeOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "桌型编码")
    private String code;

    @Schema(description = "桌型中文说明")
    private String name;

    @Schema(description = "该桌型最少接待人数")
    private Integer minPeople;

    @Schema(description = "该桌型最多接待人数")
    private Integer maxPeople;

    @Schema(description = "该桌型桌数")
    private Integer tableCount;

    @Schema(description = "单桌最大可容纳人数，来自 store_table")
    private Integer maxCapacity;
}
