package com.comeoften.module.home.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 轮播保存请求。时间格式 yyyy-MM-dd HH:mm:ss，留空表示长期有效。
 */
@Data
@Schema(description = "轮播保存请求")
public class BannerSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "轮播 ID，为空表示新增")
    private Long id;

    @NotBlank(message = "轮播标题不能为空")
    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "图片对象键不能为空")
    @Schema(description = "图片对象键，宽 750", requiredMode = Schema.RequiredMode.REQUIRED)
    private String objectKey;

    @Schema(description = "跳转类型：NONE / DISH / CATEGORY / RESERVE", example = "DISH")
    private String jumpType;

    @Schema(description = "跳转目标 ID")
    private String jumpValue;

    @Schema(description = "权重，越大越靠前", example = "10")
    private Integer weight;

    @Schema(description = "生效开始时间 yyyy-MM-dd HH:mm:ss，可空")
    private String startTime;

    @Schema(description = "生效结束时间 yyyy-MM-dd HH:mm:ss，可空")
    private String endTime;
}
