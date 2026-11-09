package com.comeoften.integration.wechat.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.comeoften.common.BizException;
import com.comeoften.common.ErrorCode;
import com.comeoften.integration.wechat.MiniAppClient;
import com.comeoften.integration.wechat.WxSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 真实微信实现。开发期商家端复用同一测试 AppID；上线申请独立 AppID 后在此按端选择 WxMaService。
 * 手机号走 getPhoneNumber 的 code 换号接口，需企业主体小程序与额度。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "comeoften.wx.mock-enabled", havingValue = "false")
public class WxJavaMiniAppClient implements MiniAppClient {

    private final WxMaService wxMaService;

    @Override
    public WxSession code2Session(String code, String mockKey) {
        try {
            WxMaJscode2SessionResult result = wxMaService.getUserService().getSessionInfo(code);
            return new WxSession(result.getOpenid(), result.getUnionid(), result.getSessionKey());
        } catch (WxErrorException e) {
            log.warn("微信 code2session 失败: {}", e.getMessage());
            throw new BizException(ErrorCode.WX_CODE_INVALID, "微信登录凭证无效或已过期", e);
        }
    }

    @Override
    public String getPhoneNumber(String phoneCode, String mockPhone) {
        if (!StringUtils.hasText(phoneCode)) {
            throw new BizException(ErrorCode.WX_PHONE_INVALID);
        }
        try {
            WxMaPhoneNumberInfo info = wxMaService.getUserService().getNewPhoneNoInfo(phoneCode);
            String phone = info == null ? null : info.getPurePhoneNumber();
            if (!StringUtils.hasText(phone)) {
                throw new BizException(ErrorCode.WX_PHONE_INVALID);
            }
            return phone;
        } catch (WxErrorException e) {
            log.warn("微信获取手机号失败: {}", e.getMessage());
            throw new BizException(ErrorCode.WX_PHONE_INVALID, "微信手机号获取失败，请重新授权", e);
        }
    }
}
