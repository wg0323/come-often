package com.comeoften.module.auth.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/** PC 后台可见菜单项。 */
@Data
@Schema(description = "菜单项")
public class MenuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "菜单 ID")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "父菜单 ID，0 为根")
    private Long parentId;

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "权限标识")
    private String perm;

    @Schema(description = "类型：0-目录，1-页面，2-按钮")
    private Integer type;

    @Schema(description = "排序值")
    private Integer sort;
}
