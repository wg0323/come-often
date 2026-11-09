package com.comeoften.module.reservation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 后台拒绝预约，必填原因，写入状态日志方便回溯。
 */
@Data
@Schema(description = "拒绝预约请求")
public class ReservationRejectDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "拒绝原因不能为空")
    @Size(max = 255, message = "拒绝原因不超过 255 字")
    @Schema(description = "拒绝原因", required = true, example = "该时段包间已满")
    private String remark;
}
