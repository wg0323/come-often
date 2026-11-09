package com.comeoften.module.reservation.constant;

/**
 * 桌型与人数区间（计划 3.8）。区间是业务规则，桌台数量与单桌容量一律来自 store_table，
 * 代码不写死任何桌号与容量。
 */
public enum ReservationTableType {

    HALL_CARD("HALL_CARD", "大厅卡座", 1, 4),

    PRIVATE_SMALL("PRIVATE_SMALL", "包间（6-10 人）", 6, 10),

    PRIVATE_LARGE("PRIVATE_LARGE", "大包间（12-20 人）", 12, 20),

    WINDOW("WINDOW", "靠窗", 1, 4);

    private final String code;

    private final String description;

    private final int minPeople;

    private final int maxPeople;

    ReservationTableType(String code, String description, int minPeople, int maxPeople) {
        this.code = code;
        this.description = description;
        this.minPeople = minPeople;
        this.maxPeople = maxPeople;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getMinPeople() {
        return minPeople;
    }

    public int getMaxPeople() {
        return maxPeople;
    }

    public static ReservationTableType of(String code) {
        if (code == null) {
            return null;
        }
        for (ReservationTableType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static String describe(String code) {
        ReservationTableType type = of(code);
        return type == null ? code : type.getDescription();
    }

    /**
     * 人数是否落在该桌型的接待区间内。8 人不能占用 12-20 人的大包间，
     * 除非后台把该包间在小程序端显式放开（M7 未开放该开关，按区间硬校验）。
     */
    public static boolean supports(String code, int peopleCount) {
        ReservationTableType type = of(code);
        return type != null && peopleCount >= type.minPeople && peopleCount <= type.maxPeople;
    }

    /**
     * 按人数推荐桌型：≤4 卡座，6-10 小包间，12-20 大包间。无匹配返回 null。
     */
    public static String recommend(int peopleCount) {
        if (peopleCount >= HALL_CARD.minPeople && peopleCount <= HALL_CARD.maxPeople) {
            return HALL_CARD.code;
        }
        if (peopleCount >= PRIVATE_SMALL.minPeople && peopleCount <= PRIVATE_SMALL.maxPeople) {
            return PRIVATE_SMALL.code;
        }
        if (peopleCount >= PRIVATE_LARGE.minPeople && peopleCount <= PRIVATE_LARGE.maxPeople) {
            return PRIVATE_LARGE.code;
        }
        return null;
    }
}
