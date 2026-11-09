package com.comeoften.integration.wechat;

/**
 * 小程序登录与手机号获取抽象。mockKey、mockPhone 仅 dev Mock 实现使用，
 * 用于在没有真实 AppSecret 与企业主体小程序时仍能复现「换手机号即换会员」的验收路径。
 */
public interface MiniAppClient {

    WxSession code2Session(String code, String mockKey);

    /**
     * 用 getPhoneNumber 回调携带的 code 换手机号。
     *
     * @param phoneCode 小程序 button open-type="getPhoneNumber" 回调的 code
     * @param mockPhone 本地手工指定的手机号，真实实现忽略该参数
     * @return 纯手机号，不含国家码
     */
    String getPhoneNumber(String phoneCode, String mockPhone);
}
