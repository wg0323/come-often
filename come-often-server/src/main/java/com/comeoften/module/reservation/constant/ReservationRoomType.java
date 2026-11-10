package com.comeoften.module.reservation.constant;

/**
 * 包间类型（store_table.room_type）。空串表示还没归类的普通桌台，不参与「指定包间」。
 * PRIVATE_SMALL / PRIVATE_LARGE 与桌型枚举同码值，便于预约时把桌型偏好与具体包间对齐。
 */
public enum ReservationRoomType {

    HALL("大厅"),

    PRIVATE_SMALL("包间"),

    PRIVATE_LARGE("大包间");

    private final String description;

    ReservationRoomType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static ReservationRoomType of(String code) {
        if (code == null) {
            return null;
        }
        for (ReservationRoomType type : values()) {
            if (type.name().equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static boolean isValid(String code) {
        return of(code) != null;
    }

    public static boolean isPrivate(String code) {
        return PRIVATE_SMALL.name().equals(code) || PRIVATE_LARGE.name().equals(code);
    }

    public static String describe(String code) {
        ReservationRoomType type = of(code);
        return type == null ? "" : type.getDescription();
    }
}
