package com.comeoften.module.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 后台员工表，PC 后台与商家端小程序共用同一套账号。
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String username;

    private String passwordHash;

    private String realName;

    private String phone;

    private String wxOpenid;

    private LocalDateTime lastLoginTime;

    private Integer status;

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
