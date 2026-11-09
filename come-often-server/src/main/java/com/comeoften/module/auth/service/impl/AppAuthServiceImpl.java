package com.comeoften.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.integration.wechat.MiniAppClient;
import com.comeoften.integration.wechat.WxSession;
import com.comeoften.module.auth.converter.AuthConverter;
import com.comeoften.module.auth.dto.AppLoginDTO;
import com.comeoften.module.auth.entity.Member;
import com.comeoften.module.auth.mapper.MemberMapper;
import com.comeoften.module.auth.service.AppAuthService;
import com.comeoften.module.auth.service.AuthTokenService;
import com.comeoften.module.auth.vo.LoginVO;
import com.comeoften.module.auth.vo.MemberInfoVO;
import com.comeoften.security.ClientType;
import com.comeoften.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 按手机号 upsert 会员并签发顾客端 token。
 *
 * <p>手机号是账号唯一标识：换微信、换手机卡、同一人多个手机号，落到哪个手机号就是哪个会员；
 * openid 只记录最近一次登录，不再参与身份判定，避免换微信后历史订单丢失。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppAuthServiceImpl implements AppAuthService {

    private static final String DEFAULT_NICKNAME = "微信用户";

    private static final Integer STATUS_ENABLED = 1;

    private final MiniAppClient miniAppClient;

    private final MemberMapper memberMapper;

    private final AuthTokenService authTokenService;

    @Override
    public LoginVO login(AppLoginDTO dto) {
        String phone = miniAppClient.getPhoneNumber(dto.getPhoneCode(), dto.getMockPhone());
        WxSession session = resolveSession(dto);
        Member member = memberMapper.selectOne(new LambdaQueryWrapper<Member>()
                .eq(Member::getPhone, phone));
        if (member == null) {
            member = create(phone, session);
        } else {
            refreshLoginTrace(member, session);
        }
        if (!STATUS_ENABLED.equals(member.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        return authTokenService.issue(ClientType.APP, member.getId(), null);
    }

    @Override
    public void logout(LoginUser loginUser) {
        authTokenService.logout(loginUser);
    }

    @Override
    public MemberInfoVO info(Long memberId) {
        Member member = memberMapper.selectById(memberId);
        if (member == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return AuthConverter.toMemberInfoVO(member);
    }

    /** openid 只用于记录，微信侧失败不影响手机号登录。 */
    private WxSession resolveSession(AppLoginDTO dto) {
        if (!StringUtils.hasText(dto.getCode()) && !StringUtils.hasText(dto.getMockKey())) {
            return null;
        }
        try {
            return miniAppClient.code2Session(dto.getCode(), dto.getMockKey());
        } catch (BizException e) {
            log.warn("顾客登录获取 openid 失败，按手机号继续: {}", e.getMessage());
            return null;
        }
    }

    private Member create(String phone, WxSession session) {
        Member member = new Member();
        member.setPhone(phone);
        member.setOpenid(session == null ? null : session.getOpenid());
        member.setUnionId(session == null ? null : session.getUnionId());
        member.setNickname(DEFAULT_NICKNAME);
        member.setAvatar("");
        member.setStatus(STATUS_ENABLED);
        try {
            memberMapper.insert(member);
            return member;
        } catch (DuplicateKeyException e) {
            log.info("会员并发注册命中手机号唯一索引，改为查询: phone={}", phone);
            return memberMapper.selectOne(new LambdaQueryWrapper<Member>()
                    .eq(Member::getPhone, phone));
        }
    }

    /** 同一手机号换微信号登录时，把 openid 刷新成最近一次，历史数据仍归这个会员。 */
    private void refreshLoginTrace(Member member, WxSession session) {
        if (session == null) {
            return;
        }
        boolean changed = false;
        if (StringUtils.hasText(session.getOpenid())
                && !Objects.equals(session.getOpenid(), member.getOpenid())) {
            member.setOpenid(session.getOpenid());
            changed = true;
        }
        if (StringUtils.hasText(session.getUnionId())
                && !Objects.equals(session.getUnionId(), member.getUnionId())) {
            member.setUnionId(session.getUnionId());
            changed = true;
        }
        if (changed) {
            memberMapper.updateById(member);
        }
    }
}
