package com.comeoften.module.store.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 门店信息，含营业时间与桌台。列表与详情共用。
 */
@Data
@Schema(description = "门店信息")
public class StoreVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "门店 ID")
    private Long id;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "门店电话")
    private String phone;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "营业状态：0-休息中，1-营业中")
    private Integer businessStatus;

    @Schema(description = "营业状态文案", example = "营业中")
    private String businessStatusText;

    @Schema(description = "营业时间，按星期升序")
    private List<BusinessHourVO> businessHours;

    @Schema(description = "桌台清单")
    private List<StoreTableVO> tables;

    /**
     * 单日营业时间。
     */
    @Data
    @Schema(description = "单日营业时间")
    public static class BusinessHourVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "记录 ID")
        private Long id;

        @Schema(description = "星期：1-周一，7-周日")
        private Integer weekDay;

        @Schema(description = "星期文案", example = "周一")
        private String weekDayText;

        @Schema(description = "开始营业时间 HH:mm")
        private String openTime;

        @Schema(description = "结束营业时间 HH:mm")
        private String closeTime;
    }

    /**
     * 桌台。
     */
    @Data
    @Schema(description = "桌台")
    public static class StoreTableVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "桌台 ID")
        private Long id;

        @Schema(description = "桌号或包间名")
        private String tableNo;

        @Schema(description = "桌型编码")
        private String tableType;

        @Schema(description = "可容纳人数")
        private Integer capacity;

        @Schema(description = "状态：0-空闲，1-占用，2-已预约")
        private Integer status;
    }
}
