package com.comeoften.module.merchant.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 商家端新订单提醒结果。模板 ID 未配置时降级为落 message_record + 日志，
 * sent 恒为 false，channel 说明实际走了哪条路径。
 */
@Data
@Schema(description = "商家端新订单提醒结果")
public class MerchantNotifyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否真正下发订阅消息；M9 未接发送通道，恒为 false")
    private Boolean sent;

    @Schema(description = "实际通道：LOG / SUBSCRIBE")
    private String channel;

    @Schema(description = "商家端订阅消息模板是否已配置")
    private Boolean templateConfigured;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "提醒内容")
    private String content;
}
