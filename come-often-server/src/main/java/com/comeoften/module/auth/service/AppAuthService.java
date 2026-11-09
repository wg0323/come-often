package com.comeoften.module.auth.service;

import com.comeoften.module.auth.dto.AppLoginDTO;
import com.comeoften.module.auth.vo.LoginVO;
import com.comeoften.module.auth.vo.MemberInfoVO;
import com.comeoften.security.LoginUser;

/** 顾客端手机号登录与退出。 */
public interface AppAuthService {

    LoginVO login(AppLoginDTO dto);

    void logout(LoginUser loginUser);

    MemberInfoVO info(Long memberId);
}
