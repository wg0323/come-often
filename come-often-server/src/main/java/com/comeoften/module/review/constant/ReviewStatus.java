package com.comeoften.module.review.constant;

/**
 * 评价审核状态。先发后审：新建评价直接 APPROVED，历史评价靠 DB 默认值兼容；
 * 只有 APPROVED 计入评分聚合与前台评价列表，PENDING 是后台复核中的下架态。
 */
public enum ReviewStatus {

    PENDING("待审核"),

    APPROVED("已通过"),

    REJECTED("已拒绝");

    private final String description;

    ReviewStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static ReviewStatus of(String code) {
        if (code == null) {
            return null;
        }
        for (ReviewStatus status : values()) {
            if (status.name().equals(code)) {
                return status;
            }
        }
        return null;
    }

    public static String describe(String code) {
        ReviewStatus status = of(code);
        return status == null ? "" : status.getDescription();
    }
}
