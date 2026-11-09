package com.comeoften.integration.notify;

import lombok.Data;

import java.io.Serializable;

/**
 * 通知发送结果。{@code sent=false} 表示只落库未真正下发（当前 LOG 通道与模板缺失时的降级都是这种）。
 */
@Data
public class NotifySendResult implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String CHANNEL_LOG = "LOG";

    public static final String CHANNEL_WX_SUBSCRIBE = "WX_SUBSCRIBE";

    private String channel;

    private boolean sent;

    public static NotifySendResult degraded(String channel) {
        NotifySendResult result = new NotifySendResult();
        result.setChannel(channel);
        result.setSent(false);
        return result;
    }
}
