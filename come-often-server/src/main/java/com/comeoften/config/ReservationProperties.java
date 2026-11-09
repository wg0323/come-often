package com.comeoften.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 预约域配置。时段、可预约桌型、提前天数、取消限制与超时分钟数都是运行时参数，
 * M0 没有配置表，M7 不新增表，全部走 comeoften.reservation.*。
 */
@Data
@Component
@ConfigurationProperties(prefix = "comeoften.reservation")
public class ReservationProperties {

    /** 可预约未来天数，含当天。 */
    private int daysAhead = 7;

    /** 开放预约的时段，格式 HH:mm。 */
    private List<String> timeSlots = new ArrayList<>(Arrays.asList(
            "11:00", "11:30", "12:00", "12:30", "13:00", "13:30",
            "17:00", "17:30", "18:00", "18:30", "19:00", "19:30", "20:00"));

    /** 开放预约的桌型，必须是 ReservationTableType 中的编码。 */
    private List<String> tableTypes = new ArrayList<>(Arrays.asList(
            "HALL_CARD", "PRIVATE_SMALL", "PRIVATE_LARGE"));

    /** 距预约时间不足该小时数不可取消。 */
    private long cancelLimitHours = 2L;

    /** 预约时间过后多少分钟仍未签到置为 NO_SHOW。 */
    private int noShowMinutes = 15;

    /** 预约前多少分钟发提醒。 */
    private int remindAheadMinutes = 60;

    /** 下单后是否自动确认；关闭后预约单停在 PENDING 等后台审核。 */
    private boolean autoConfirm = true;

    /** 未到店兜底扫描 cron，实时事项仍走延迟消息。 */
    private String noShowScanCron = "0 */5 * * * ?";

    /** 兜底扫描单批条数。 */
    private int noShowScanBatchSize = 200;
}
