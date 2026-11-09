package com.comeoften.module.points.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员等级与权益表。门槛按「年度消费额 + 累计积分」双条件判定，两者同时满足才升级，
 * 折扣率在 M6 只作权益展示，未参与订单金额计算（见 deploy/init.md 差异）。
 */
@Data
@TableName("member_level")
public class MemberLevel implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String levelName;

    private String levelCode;

    private BigDecimal minAmount;

    private Integer minPoints;

    private BigDecimal discountRate;

    private Integer sort;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
