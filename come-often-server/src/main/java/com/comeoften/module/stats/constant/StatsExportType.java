package com.comeoften.module.stats.constant;

/**
 * 报表导出类型。一个类型一个工作簿，字段与页面上同名表格一一对应，
 * 导出走报表页权限点 stats:view，不新造权限。
 */
public enum StatsExportType {

    TREND("trend", "营收趋势", "营收趋势"),

    HOT_DISH("hot-dish", "热销排行", "热销排行"),

    COUPON_ROI("coupon-roi", "券ROI", "券ROI"),

    HIGH_VALUE_MEMBER("high-value-member", "高价值用户", "高价值用户");

    private final String code;

    private final String sheetName;

    private final String fileLabel;

    StatsExportType(String code, String sheetName, String fileLabel) {
        this.code = code;
        this.sheetName = sheetName;
        this.fileLabel = fileLabel;
    }

    public String getCode() {
        return code;
    }

    public String getSheetName() {
        return sheetName;
    }

    public String getFileLabel() {
        return fileLabel;
    }

    public static StatsExportType of(String code) {
        if (code == null) {
            return null;
        }
        for (StatsExportType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        return null;
    }
}
