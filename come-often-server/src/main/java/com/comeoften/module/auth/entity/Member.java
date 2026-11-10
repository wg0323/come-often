package com.comeoften.module.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 顾客会员表。 */
@Data
@TableName("member")
public class Member implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String openid;

    private String unionId;

    private String phone;

    private String nickname;

    private String avatar;

    private Long levelId;

    private LocalDate birthday;

    private LocalDateTime lastVisitTime;

    private Integer status;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
