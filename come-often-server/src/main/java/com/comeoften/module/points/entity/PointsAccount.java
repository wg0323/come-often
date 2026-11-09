package com.comeoften.module.points.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 积分账户表，一个会员一行，余额是最终事实源。 */
@Data
@TableName("points_account")
public class PointsAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long memberId;

    private Integer balance;

    private Integer totalEarned;

    private Integer totalUsed;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
