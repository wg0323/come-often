package com.comeoften.module.dish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 顾客端菜品搜索条件。门店由服务端覆盖，页面只回传关键词与分页。
 */
@Data
@Schema(description = "菜品搜索条件")
public class DishSearchDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "门店 ID，为空回退到当前唯一门店")
    private Long storeId;

    @Schema(description = "搜索关键词，最多 30 字；右模糊匹配菜名、副标题、描述与标签；长度由服务层校验")
    private String keyword;

    @Schema(description = "当前页，从 1 开始")
    private Long current;

    @Schema(description = "每页条数，最大 20")
    private Long size;
}
