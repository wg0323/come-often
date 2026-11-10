package com.comeoften.module.bundle.constant;

/**
 * 套餐上下架状态。与 dish.status 同码值，避免后台另记一套。
 */
public enum BundleStatus {

    OFF_SHELF(0, "下架"),

    ON_SHELF(1, "上架");

    private final int code;

    private final String description;

    BundleStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static BundleStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (BundleStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    public static boolean isOnShelf(Integer code) {
        return ON_SHELF.code == (code == null ? OFF_SHELF.code : code);
    }
}
