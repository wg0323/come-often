package com.comeoften.integration.wechat;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * code2session 结果。sessionKey 只在服务端使用，不下发给小程序。
 */
@Data
@AllArgsConstructor
public class WxSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private String openid;

    private String unionId;

    private String sessionKey;
}
