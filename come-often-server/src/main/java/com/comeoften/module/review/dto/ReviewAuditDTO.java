package com.comeoften.module.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 后台评价审核。目标状态三态都允许，重复提交同一状态返回 30087。
 */
@Data
@Schema(description = "评价审核请求")
public class ReviewAuditDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "目标审核状态不能为空")
    @Schema(description = "目标状态：PENDING / APPROVED / REJECTED", requiredMode = Schema.RequiredMode.REQUIRED, example = "APPROVED")
    private String status;

    @Size(max = 255, message = "审核说明不超过 255 字")
    @Schema(description = "审核说明，写入订单状态日志之外的备注字段留作后续扩展")
    private String remark;
}
