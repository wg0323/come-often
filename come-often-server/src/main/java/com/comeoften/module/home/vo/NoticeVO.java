package com.comeoften.module.home.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 首页公告，单行滚动。
 */
@Data
@Schema(description = "首页公告")
public class NoticeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "公告 ID")
    private Long id;

    @Schema(description = "公告内容")
    private String content;

    @Schema(description = "类型：TODAY / CLOSURE / NORMAL")
    private String noticeType;
}
