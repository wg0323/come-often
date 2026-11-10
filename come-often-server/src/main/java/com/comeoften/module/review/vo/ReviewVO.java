package com.comeoften.module.review.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价展示。会员昵称已脱敏，图片只回 objectKey，由端上拼只读地址。
 */
@Data
@Schema(description = "评价")
public class ReviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "评价 ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "评价菜品 ID，未指定菜品时为空")
    private Long dishId;

    @Schema(description = "评价菜品名称，未指定菜品时为空")
    private String dishName;

    @Schema(description = "会员昵称（已脱敏）")
    private String nickname;

    @Schema(description = "评分 1~5")
    private Integer rating;

    @Schema(description = "评价内容")
    private String content;

    @Schema(description = "是否带图")
    private Boolean withImage;

    @Schema(description = "评价图 objectKey 列表")
    private List<String> imageObjectKeys;

    @Schema(description = "审核状态：PENDING/APPROVED/REJECTED")
    private String status;

    @Schema(description = "审核状态文案")
    private String statusText;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "审核员工 ID")
    private Long auditUserId;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "订单号，后台审核页展示用")
    private String orderNo;

    @JsonFormat(pattern = TIME_PATTERN)
    @Schema(description = "评价时间")
    private LocalDateTime createTime;
}
