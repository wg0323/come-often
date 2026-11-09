package com.comeoften.module.auth.service.impl;

import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.module.auth.entity.Member;
import com.comeoften.module.auth.entity.SysUser;
import com.comeoften.module.auth.mapper.MemberMapper;
import com.comeoften.module.auth.mapper.SysUserMapper;
import com.comeoften.security.AccountStateChecker;
import com.comeoften.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 身份复核：停用账号、清空微信绑定后，即使 JWT 与 Redis 都未过期也立即拒绝。
 */
@Service
@RequiredArgsConstructor
public class AccountStateCheckerImpl implements AccountStateChecker {

    private static final Integer STATUS_ENABLED = 1;

    private final MemberMapper memberMapper;

    private final SysUserMapper sysUserMapper;

    @Override
    public void assertActive(LoginUser loginUser) {
        switch (loginUser.getClientType()) {
            case APP:
                Member member = memberMapper.selectById(loginUser.getUserId());
                if (member == null || !STATUS_ENABLED.equals(member.getStatus())) {
                    throw new BizException(ErrorCode.ACCOUNT_DISABLED);
                }
                break;
            case ADMIN:
                assertStaffActive(loadStaff(loginUser.getUserId()));
                break;
            case MERCHANT:
                SysUser staff = assertStaffActive(loadStaff(loginUser.getUserId()));
                if (!StringUtils.hasText(staff.getWxOpenid())) {
                    throw new BizException(ErrorCode.UNAUTHORIZED, "微信绑定已解除，请重新登录");
                }
                break;
            default:
                throw new BizException(ErrorCode.UNAUTHORIZED);
        }
    }

    private SysUser loadStaff(Long userId) {
        SysUser staff = sysUserMapper.selectById(userId);
        if (staff == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return staff;
    }

    private SysUser assertStaffActive(SysUser staff) {
        if (!STATUS_ENABLED.equals(staff.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        return staff;
    }
}
