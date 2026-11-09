package com.comeoften.module.auth.service;

import com.comeoften.module.auth.vo.MenuVO;

import java.util.List;
import java.util.Set;

/** RBAC 查询：员工-角色-菜单-权限点。 */
public interface PermissionService {

    Set<String> listPermissions(Long userId);

    List<MenuVO> listMenus(Long userId);

    List<String> listRoleCodes(Long userId);

    Long firstStoreId(Long userId);
}
