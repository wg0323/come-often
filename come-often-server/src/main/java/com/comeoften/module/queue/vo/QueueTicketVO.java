package com.comeoften.module.queue.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 排队号视图。waitingCount 是仍排在前面且未叫号的数量，由服务端算好。
 */
@Data
@Schema(description = "排队号")
public class QueueTicketVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Schema(description = "排队号，如 Q001")
    private String ticketNo;

    @Schema(description = "就餐人数")
    private Integer partySize;

    @Schema(description = "状态编码：WAITING/CALLED/SEATED/CANCELLED/EXPIRED")
    private String status;

    @Schema(description = "状态中文说明")
    private String statusText;

    @Schema(description = "前面还在等待的桌数")
    private Long waitingCount;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "取号时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "叫号时间")
    private LocalDateTime calledTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "营业日")
    private LocalDate bizDate;

    @Schema(description = "会员脱敏手机号，顾客端为空")
    private String memberPhone;
}
