package com.comeoften.module.points.constant;

/**
 * 积分变动方向，取值与 points_record.change_type 的注释一致。
 *
 * <p>抵现走「下单 FREEZE → 支付成功 USE → 取消/超时 ROLLBACK」三段，不做裸 UPDATE 余额扣减；
 * 过期由每日批处理写 EXPIRE 反向流水。</p>
 */
public enum PointsChangeType {

    EARN("EARN", "获得"),
    USE("USE", "实扣"),
    FREEZE("FREEZE", "冻结"),
    ROLLBACK("ROLLBACK", "退回"),
    EXPIRE("EXPIRE", "过期");

    private final String code;

    private final String description;

    PointsChangeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 按码值解析，未匹配返回 null（历史数据不抛异常）。
     */
    public static PointsChangeType of(String code) {
        for (PointsChangeType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
