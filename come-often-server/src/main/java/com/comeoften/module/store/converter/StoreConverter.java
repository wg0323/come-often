package com.comeoften.module.store.converter;

import com.comeoften.module.store.entity.Store;
import com.comeoften.module.store.entity.StoreBusinessHour;
import com.comeoften.module.store.entity.StoreTable;
import com.comeoften.module.store.vo.StoreVO;
import org.springframework.beans.BeanUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 门店对象转换。桌号与容量不做任何硬编码映射，原样透出。
 */
public final class StoreConverter {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final String[] WEEK_DAY_TEXT = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private static final String[] STATUS_TEXT = {"休息中", "营业中"};

    private StoreConverter() {
    }

    public static StoreVO toVO(Store store, List<StoreBusinessHour> hours, List<StoreTable> tables) {
        StoreVO vo = new StoreVO();
        BeanUtils.copyProperties(store, vo);
        int status = store.getBusinessStatus() == null ? 0 : store.getBusinessStatus();
        vo.setBusinessStatusText(STATUS_TEXT[status == 1 ? 1 : 0]);
        vo.setBusinessHours(hours.stream().map(StoreConverter::toBusinessHourVO).collect(Collectors.toList()));
        vo.setTables(tables.stream().map(StoreConverter::toTableVO).collect(Collectors.toList()));
        return vo;
    }

    public static StoreVO.BusinessHourVO toBusinessHourVO(StoreBusinessHour hour) {
        StoreVO.BusinessHourVO vo = new StoreVO.BusinessHourVO();
        vo.setId(hour.getId());
        vo.setWeekDay(hour.getWeekDay());
        int day = hour.getWeekDay() == null ? 0 : hour.getWeekDay();
        vo.setWeekDayText(day >= 1 && day <= 7 ? WEEK_DAY_TEXT[day] : "");
        vo.setOpenTime(hour.getOpenTime() == null ? "" : hour.getOpenTime().format(TIME_FORMATTER));
        vo.setCloseTime(hour.getCloseTime() == null ? "" : hour.getCloseTime().format(TIME_FORMATTER));
        return vo;
    }

    public static StoreVO.StoreTableVO toTableVO(StoreTable table) {
        StoreVO.StoreTableVO vo = new StoreVO.StoreTableVO();
        BeanUtils.copyProperties(table, vo);
        return vo;
    }
}
