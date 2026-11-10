package com.comeoften.module.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户持有的券。code_seq 为动态券码的短序号，签名在服务端按时间窗现算，不落库防截图。
 */
@Data
@TableName("user_coupon")
public class UserCoupon implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long templateId;
    private Long memberId;
    private Long storeId;
    private String codeSeq;
    private Integer status;
    private LocalDateTime receiveTime;
    private LocalDateTime useTime;
    private LocalDateTime expireAt;
    private Long orderId;
    /** 规则发券幂等键，手动领券为空；沿用 M6 的 # 用途后缀口径。 */
    private String grantBizId;
    private Integer isDeleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
