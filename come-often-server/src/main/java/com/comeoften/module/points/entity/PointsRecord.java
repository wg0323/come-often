package com.comeoften.module.points.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分流水表。(member_id, biz_type, biz_id) 唯一索引是消费幂等的最终判据，
 * change_amount 正数入账、负数出账，balance_after 记录变动后余额供对账。
 */
@Data
@TableName("points_record")
public class PointsRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long memberId;

    private String changeType;

    private Integer changeAmount;

    private String bizType;

    private String bizId;

    private Integer balanceAfter;

    private LocalDateTime expireAt;

    private String remark;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
