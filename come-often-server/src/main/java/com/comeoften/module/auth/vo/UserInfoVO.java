package com.comeoften.module.auth.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/** 当前登录员工信息，PC 菜单与商家端宫格按 permissions 渲染。 */
@Data
@Schema(description = "当前登录员工信息")
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "员工 ID")
    private Long userId;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "员工姓名")
    private String realName;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "所属门店 ID")
    private Long storeId;

    @Schema(description = "角色编码列表")
    private List<String> roles;

    @Schema(description = "权限标识列表")
    private List<String> permissions;
}
