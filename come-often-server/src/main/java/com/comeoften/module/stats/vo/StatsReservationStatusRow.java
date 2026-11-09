package com.comeoften.module.stats.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 内部聚合行：某日各状态预约单数。
 */
@Data
public class StatsReservationStatusRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private String status;

    private Long count;
}
