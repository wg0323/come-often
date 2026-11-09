package com.comeoften.module.auth.service;

import com.comeoften.module.auth.vo.LoginVO;
import com.comeoften.security.ClientType;
import com.comeoften.security.LoginUser;

/** 签发并登记当前有效凭证，供三端登录共用。 */
public interface AuthTokenService {

    LoginVO issue(ClientType clientType, Long userId, Long storeId);

    void logout(LoginUser loginUser);
}
