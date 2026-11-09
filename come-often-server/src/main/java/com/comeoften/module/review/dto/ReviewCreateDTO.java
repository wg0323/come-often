package com.comeoften.module.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 提交评价。订单号定位订单，dishId 可空表示只评订单不指定菜品，
 * 图片只传已上传的 objectKey，不传完整 URL。
 */
@Data
@Schema(description = "提交评价")
public class ReviewCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "订单号不能为空")
    @Schema(description = "订单号", required = true)
    private String orderNo;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分范围 1~5")
    @Max(value = 5, message = "评分范围 1~5")
    @Schema(description = "评分 1~5", required = true, example = "5")
    private Integer rating;

    @Size(max = 500, message = "评价内容最多 500 字")
    @Schema(description = "评价内容，最多 500 字")
    private String content;

    @Schema(description = "评价菜品 ID，可空；有值时必须属于该订单")
    private Long dishId;

    @Size(max = 3, message = "评价图最多 3 张")
    @Schema(description = "评价图 objectKey 列表，最多 3 张，前缀必须是 review/，且对象必须已上传")
    private List<String> imageObjectKeys;
}
