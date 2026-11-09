package com.comeoften.module.home.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 首页轮播。
 */
@Data
@Schema(description = "首页轮播")
public class BannerVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "轮播 ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "图片 objectKey，宽 750")
    private String objectKey;

    @Schema(description = "跳转类型：NONE / DISH / CATEGORY / RESERVE")
    private String jumpType;

    @Schema(description = "跳转目标 ID")
    private String jumpValue;

    @Schema(description = "权重，越大越靠前")
    private Integer weight;
}
