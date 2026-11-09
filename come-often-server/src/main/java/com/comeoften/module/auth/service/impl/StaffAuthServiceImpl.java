package com.comeoften.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.integration.wechat.MiniAppClient;
import com.comeoften.integration.wechat.WxSession;
import com.comeoften.module.auth.converter.AuthConverter;
import com.comeoften.module.auth.dto.AccountLoginDTO;
import com.comeoften.module.auth.dto.MerchantLoginDTO;
import com.comeoften.module.auth.dto.WxLoginDTO;
import com.comeoften.module.auth.entity.SysUser;
import com.comeoften.module.auth.mapper.SysUserMapper;
import com.comeoften.module.auth.service.AuthTokenService;
import com.comeoften.module.auth.service.PermissionService;
import com.comeoften.module.auth.service.StaffAuthService;
import com.comeoften.module.auth.vo.LoginVO;
import com.comeoften.module.auth.vo.MenuVO;
import com.comeoften.module.auth.vo.UserInfoVO;
import com.comeoften.security.ClientType;
import com.comeoften.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/** 员工登录、微信绑定与免密登录；PC 与商家端共用 sys_user 与同一套 RBAC。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffAuthServiceImpl implements StaffAuthService {

    private static final Integer STATUS_ENABLED = 1;

    private final SysUserMapper sysUserMapper;

    private final MiniAppClient miniAppClient;

    private final PasswordEncoder passwordEncoder;

    private final AuthTokenService authTokenService;

    private final PermissionService permissionService;

    @Override
    public LoginVO adminLogin(AccountLoginDTO dto) {
        SysUser user = loadByUsername(dto.getUsername());
        verifyPassword(dto.getPassword(), user);
        assertEnabled(user);
        Long storeId = requireStore(user.getId());
        touchLastLogin(user.getId());
        return authTokenService.issue(ClientType.ADMIN, user.getId(), storeId);
    }

    @Override
    public LoginVO merchantLogin(MerchantLoginDTO dto) {
        SysUser user = loadByUsername(dto.getUsername());
        verifyPassword(dto.getPassword(), user);
        assertEnabled(user);
        String openid = resolveOpenid(dto.getCode(), dto.getMockKey());
        bindOpenid(user, openid);
        Long storeId = requireStore(user.getId());
        touchLastLogin(user.getId());
        return authTokenService.issue(ClientType.MERCHANT, user.getId(), storeId);
    }

    @Override
    public LoginVO merchantAutoLogin(WxLoginDTO dto) {
        String openid = resolveOpenid(dto.getCode(), dto.getMockKey());
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getWxOpenid, openid));
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未绑定员工账号，请使用账号密码登录");
        }
        assertEnabled(user);
        Long storeId = requireStore(user.getId());
        touchLastLogin(user.getId());
        return authTokenService.issue(ClientType.MERCHANT, user.getId(), storeId);
    }

    @Override
    public UserInfoVO info(LoginUser loginUser) {
        SysUser user = sysUserMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        List<String> permissions = permissionService.listPermissions(user.getId()).stream()
                .sorted()
                .collect(Collectors.toList());
        return AuthConverter.toUserInfoVO(user, loginUser.getStoreId(),
                permissionService.listRoleCodes(user.getId()), permissions);
    }

    @Override
    public LoginVO refresh(LoginUser loginUser) {
        return authTokenService.issue(loginUser.getClientType(), loginUser.getUserId(), loginUser.getStoreId());
    }

    @Override
    public List<MenuVO> menus(Long userId) {
        return permissionService.listMenus(userId);
    }

    @Override
    public void logout(LoginUser loginUser) {
        authTokenService.logout(loginUser);
    }

    private SysUser loadByUsername(String username) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }
        return user;
    }

    private void verifyPassword(String rawPassword, SysUser user) {
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }
    }

    private void assertEnabled(SysUser user) {
        if (!STATUS_ENABLED.equals(user.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
    }

    private Long requireStore(Long userId) {
        Long storeId = permissionService.firstStoreId(userId);
        if (storeId == null) {
            throw new BizException(ErrorCode.STORE_UNBOUND);
        }
        return storeId;
    }

    private String resolveOpenid(String code, String mockKey) {
        WxSession session = miniAppClient.code2Session(code, mockKey);
        if (!StringUtils.hasText(session.getOpenid())) {
            throw new BizException(ErrorCode.WX_CODE_INVALID);
        }
        return session.getOpenid();
    }

    private void bindOpenid(SysUser user, String openid) {
        SysUser bound = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getWxOpenid, openid));
        if (bound != null && !bound.getId().equals(user.getId())) {
            throw new BizException(ErrorCode.WX_ALREADY_BOUND);
        }
        if (openid.equals(user.getWxOpenid())) {
            return;
        }
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setWxOpenid(openid);
        try {
            sysUserMapper.updateById(update);
        } catch (DuplicateKeyException e) {
            log.warn("微信绑定并发冲突: userId={}", user.getId());
            throw new BizException(ErrorCode.WX_ALREADY_BOUND, "该微信已绑定其他员工", e);
        }
    }

    private void touchLastLogin(Long userId) {
        SysUser update = new SysUser();
        update.setId(userId);
        update.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(update);
    }
}
