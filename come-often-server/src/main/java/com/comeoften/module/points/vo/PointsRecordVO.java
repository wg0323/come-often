package com.comeoften.module.points.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分流水。changeAmount 正数为入账、负数为出账，与 points_record 一致，不做二次换算。
 */
@Data
@Schema(description = "积分流水")
public class PointsRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "流水 ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "会员 ID")
    private Long memberId;

    @Schema(description = "变动方向：EARN/USE/FREEZE/ROLLBACK/EXPIRE")
    private String changeType;

    @Schema(description = "变动方向中文说明")
    private String changeTypeText;

    @Schema(description = "变动积分，正数入账负数出账")
    private Integer changeAmount;

    @Schema(description = "业务类型：ORDER/SIGN_IN/REVIEW/SHARE/PROFILE")
    private String bizType;

    @Schema(description = "业务幂等键")
    private String bizId;

    @Schema(description = "变动后余额")
    private Integer balanceAfter;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "该笔积分过期时间；非入账流水为空")
    private LocalDateTime expireAt;

    @Schema(description = "说明")
    private String remark;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "发生时间")
    private LocalDateTime createTime;
}
