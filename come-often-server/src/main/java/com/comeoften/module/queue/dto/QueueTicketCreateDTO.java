package com.comeoften.module.queue.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 取号请求。人数区间由服务端常量校验，页面只负责收集。
 */
@Data
@Schema(description = "取号请求")
public class QueueTicketCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "请选择就餐人数")
    @Min(value = 1, message = "就餐人数至少 1 位")
    @Max(value = 20, message = "就餐人数最多 20 位")
    @Schema(description = "就餐人数，1~20 位")
    private Integer partySize;
}
