package com.comeoften.module.reservation.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预约签到码。code 每 30 秒变化一次，codeContent 带 R 前缀供扫码分流。
 */
@Data
@Schema(description = "预约签到码")
public class ReservationCodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Schema(description = "预约单号")
    private String reserveNo;

    @Schema(description = "签到码：预约单号 + 10 位签名")
    private String code;

    @Schema(description = "扫码内容，带 R 前缀供商家端扫码分流")
    private String codeContent;

    @Schema(description = "本窗口剩余秒数，页面按此设置刷新定时器")
    private Long secondsRemaining;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "预约时刻")
    private LocalDateTime reserveAt;

    @Schema(description = "预约状态中文说明")
    private String statusText;
}
