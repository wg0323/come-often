package com.comeoften.module.reservation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 桌型聚合结果，仅服务端内部使用，不直接返回前端。
 */
@Data
@Schema(description = "桌型聚合")
public class TableTypeStatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "桌型编码")
    private String tableType;

    @Schema(description = "该桌型桌数，即时段容量桶的 capacity")
    private Integer tableCount;

    @Schema(description = "该桌型单桌最小可容纳人数")
    private Integer minCapacity;

    @Schema(description = "该桌型单桌最大可容纳人数")
    private Integer maxCapacity;
}
