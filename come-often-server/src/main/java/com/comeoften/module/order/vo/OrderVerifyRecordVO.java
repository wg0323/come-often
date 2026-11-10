package com.comeoften.module.order.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 顾客端核销记录。只回本人本店订单的核销流水。
 */
@Data
@Schema(description = "订单核销记录")
public class OrderVerifyRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "记录 ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "订单 ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "核销码串快照")
    private String verifyCode;

    @Schema(description = "核销渠道编码")
    private String verifyChannel;

    @Schema(description = "核销渠道文案")
    private String channelText;

    @Schema(description = "操作方")
    private String operatorType;

    @Schema(description = "核销幂等键")
    private String bizId;

    @Schema(description = "说明")
    private String remark;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "核销时间")
    private LocalDateTime verifyTime;
}
