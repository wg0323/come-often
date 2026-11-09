package com.comeoften.module.auth.service;

import com.comeoften.module.auth.dto.AccountLoginDTO;
import com.comeoften.module.auth.dto.MerchantLoginDTO;
import com.comeoften.module.auth.dto.WxLoginDTO;
import com.comeoften.module.auth.vo.LoginVO;
import com.comeoften.module.auth.vo.MenuVO;
import com.comeoften.module.auth.vo.UserInfoVO;
import com.comeoften.security.LoginUser;

import java.util.List;

/** 员工侧登录：PC 账号登录、商家端首次绑定与免密登录。 */
public interface StaffAuthService {

    LoginVO adminLogin(AccountLoginDTO dto);

    LoginVO merchantLogin(MerchantLoginDTO dto);

    LoginVO merchantAutoLogin(WxLoginDTO dto);

    UserInfoVO info(LoginUser loginUser);

    LoginVO refresh(LoginUser loginUser);

    List<MenuVO> menus(Long userId);

    void logout(LoginUser loginUser);
}
