package com.comeoften.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 门店信息更新请求。
 */
@Data
@Schema(description = "门店信息更新请求")
public class StoreUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "门店名称不能为空")
    @Schema(description = "门店名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String storeName;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "门店电话")
    private String phone;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "营业状态：0-休息中，1-营业中", example = "1")
    private Integer businessStatus;

    @Valid
    @Schema(description = "一周营业时间；为空表示不修改，非空则按星期整体覆盖")
    private List<BusinessHourDTO> businessHours;
}
