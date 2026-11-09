package com.comeoften.module.home.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 公告保存请求。
 */
@Data
@Schema(description = "公告保存请求")
public class NoticeSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "公告 ID，为空表示新增")
    private Long id;

    @NotBlank(message = "公告内容不能为空")
    @Schema(description = "公告内容，单行滚动", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "类型：TODAY / CLOSURE / NORMAL", example = "TODAY")
    private String noticeType;

    @Schema(description = "生效开始时间 yyyy-MM-dd HH:mm:ss，可空")
    private String startTime;

    @Schema(description = "生效结束时间 yyyy-MM-dd HH:mm:ss，可空")
    private String endTime;
}
