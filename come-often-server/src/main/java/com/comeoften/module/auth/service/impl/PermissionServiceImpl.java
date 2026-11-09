package com.comeoften.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.module.auth.converter.AuthConverter;
import com.comeoften.module.auth.entity.SysMenu;
import com.comeoften.module.auth.entity.SysRole;
import com.comeoften.module.auth.entity.SysRoleMenu;
import com.comeoften.module.auth.entity.SysStoreUser;
import com.comeoften.module.auth.entity.SysUserRole;
import com.comeoften.module.auth.mapper.SysMenuMapper;
import com.comeoften.module.auth.mapper.SysRoleMapper;
import com.comeoften.module.auth.mapper.SysRoleMenuMapper;
import com.comeoften.module.auth.mapper.SysStoreUserMapper;
import com.comeoften.module.auth.mapper.SysUserRoleMapper;
import com.comeoften.module.auth.service.PermissionService;
import com.comeoften.module.auth.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 分步查询角色、菜单、权限点，不做多表 join，避免超过三表关联与 SQL 拼接。
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class PermissionServiceImpl implements PermissionService {

    private static final int MENU_TYPE_BUTTON = 2;

    private final SysUserRoleMapper sysUserRoleMapper;

    private final SysRoleMapper sysRoleMapper;

    private final SysRoleMenuMapper sysRoleMenuMapper;

    private final SysMenuMapper sysMenuMapper;

    private final SysStoreUserMapper sysStoreUserMapper;

    @Override
    public Set<String> listPermissions(Long userId) {
        return loadMenus(userId).stream()
                .map(SysMenu::getPerm)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    @Override
    public List<MenuVO> listMenus(Long userId) {
        return loadMenus(userId).stream()
                .filter(menu -> menu.getMenuType() != MENU_TYPE_BUTTON)
                .filter(menu -> StringUtils.hasText(menu.getPath()))
                .sorted(Comparator.comparing(SysMenu::getSort))
                .map(AuthConverter::toMenuVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listRoleCodes(Long userId) {
        List<Long> roleIds = loadRoleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                        .select(SysRole::getRoleCode)
                        .in(SysRole::getId, roleIds))
                .stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());
    }

    @Override
    public Long firstStoreId(Long userId) {
        return sysStoreUserMapper.selectList(new LambdaQueryWrapper<SysStoreUser>()
                        .select(SysStoreUser::getStoreId)
                        .eq(SysStoreUser::getUserId, userId))
                .stream()
                .findFirst()
                .map(SysStoreUser::getStoreId)
                .orElse(null);
    }

    private List<SysMenu> loadMenus(Long userId) {
        List<Long> roleIds = loadRoleIds(userId);
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> menuIds = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .select(SysRoleMenu::getMenuId)
                        .in(SysRoleMenu::getRoleId, roleIds))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .collect(Collectors.toList());
        if (menuIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>().in(SysMenu::getId, menuIds));
    }

    private List<Long> loadRoleIds(Long userId) {
        return sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .select(SysUserRole::getRoleId)
                        .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());
    }
}
