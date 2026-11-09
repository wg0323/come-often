package com.comeoften.integration.notify;

/**
 * 通知发送通道。计划 3.10 的两个实现：
 * {@code LogNotifySender}（默认，落 message_record + 日志）与
 * {@code WxSubscribeSender}（需模板 ID 与一次性订阅授权，当前为骨架）。
 *
 * <p>这是「Mock 能力替换清单」里的订阅消息替换点：业务侧只依赖本接口，
 * 接真实发送时换实现 + 配模板 ID，Controller / Service 不用改。</p>
 */
public interface NotifySender {

    /**
     * 发送一条通知。实现不得抛异常打断业务主链路，失败时降级为落库并返回 {@code sent=false}。
     */
    NotifySendResult send(NotifyCommand command);
}
