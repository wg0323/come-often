package com.comeoften.config;

import com.comeoften.mq.RabbitConfirmCallback;
import com.comeoften.mq.RabbitConst;
import com.comeoften.mq.RabbitReturnsCallback;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 拓扑集中声明。所有 Exchange / Queue / Binding 都在此处 @Bean 声明，durable=true，
 * 业务队列统一绑定死信交换机。可靠性三件套（本地消息表、重发补偿、死信落库）按计划在 M3 做透。
 */
@Configuration
public class RabbitConfig {

    @Bean
    public CustomExchange delayExchange() {
        Map<String, Object> args = new HashMap<>(2);
        args.put("x-delayed-type", "topic");
        return new CustomExchange(RabbitConst.DELAY_EXCHANGE, "x-delayed-message", true, false, args);
    }

    @Bean
    public Exchange orderExchange() {
        return new TopicExchange(RabbitConst.ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Exchange memberExchange() {
        return new TopicExchange(RabbitConst.MEMBER_EXCHANGE, true, false);
    }

    @Bean
    public Exchange dlxExchange() {
        return new DirectExchange(RabbitConst.DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderNotifyQueue() {
        return businessQueue(RabbitConst.ORDER_NOTIFY_QUEUE);
    }

    /**
     * M4 支付成功事件队列，供 M5 发券与 M6 积分消费；与其他业务队列同样绑定死信交换机。
     */
    @Bean
    public Queue orderPaidQueue() {
        return businessQueue(RabbitConst.ORDER_PAID_QUEUE);
    }

    @Bean
    public Queue orderTimeoutQueue() {
        return businessQueue(RabbitConst.ORDER_TIMEOUT_QUEUE);
    }

    @Bean
    public Queue reservationTimeoutQueue() {
        return businessQueue(RabbitConst.RESERVATION_TIMEOUT_QUEUE);
    }

    @Bean
    public Queue reservationRemindQueue() {
        return businessQueue(RabbitConst.RESERVATION_REMIND_QUEUE);
    }

    @Bean
    public Queue couponExpireQueue() {
        return businessQueue(RabbitConst.COUPON_EXPIRE_QUEUE);
    }

    @Bean
    public Queue deadQueue() {
        return QueueBuilder.durable(RabbitConst.DEAD_QUEUE).build();
    }

    /**
     * M6 订单完成事件队列，积分入账与等级升级在此消费；与券过期队列共用 member 交换机。
     */
    @Bean
    public Queue pointsCompletedQueue() {
        return businessQueue(RabbitConst.POINTS_COMPLETED_QUEUE);
    }

    @Bean
    public Binding orderNotifyFromOrderExchange() {
        return bind(RabbitConst.ORDER_NOTIFY_QUEUE, RabbitConst.ORDER_EXCHANGE, RabbitConst.RK_ORDER_NOTIFY);
    }

    @Bean
    public Binding orderPaidFromOrderExchange() {
        return bind(RabbitConst.ORDER_PAID_QUEUE, RabbitConst.ORDER_EXCHANGE, RabbitConst.RK_ORDER_PAID);
    }

    @Bean
    public Binding orderTimeoutFromOrderExchange() {
        return bind(RabbitConst.ORDER_TIMEOUT_QUEUE, RabbitConst.ORDER_EXCHANGE, RabbitConst.RK_ORDER_TIMEOUT);
    }

    @Bean
    public Binding couponExpireFromMemberExchange() {
        return bind(RabbitConst.COUPON_EXPIRE_QUEUE, RabbitConst.MEMBER_EXCHANGE, RabbitConst.RK_COUPON_EXPIRE);
    }

    @Bean
    public Binding pointsCompletedFromMemberExchange() {
        return bind(RabbitConst.POINTS_COMPLETED_QUEUE, RabbitConst.MEMBER_EXCHANGE,
                RabbitConst.RK_POINTS_COMPLETED);
    }

    @Bean
    public Binding orderNotifyFromDelayExchange() {
        return bind(RabbitConst.ORDER_NOTIFY_QUEUE, RabbitConst.DELAY_EXCHANGE, RabbitConst.RK_ORDER_NOTIFY);
    }

    @Bean
    public Binding orderTimeoutFromDelayExchange() {
        return bind(RabbitConst.ORDER_TIMEOUT_QUEUE, RabbitConst.DELAY_EXCHANGE, RabbitConst.RK_ORDER_TIMEOUT);
    }

    @Bean
    public Binding reservationTimeoutFromDelayExchange() {
        return bind(RabbitConst.RESERVATION_TIMEOUT_QUEUE, RabbitConst.DELAY_EXCHANGE,
                RabbitConst.RK_RESERVATION_TIMEOUT);
    }

    @Bean
    public Binding reservationRemindFromDelayExchange() {
        return bind(RabbitConst.RESERVATION_REMIND_QUEUE, RabbitConst.DELAY_EXCHANGE,
                RabbitConst.RK_RESERVATION_REMIND);
    }

    @Bean
    public Binding couponExpireFromDelayExchange() {
        return bind(RabbitConst.COUPON_EXPIRE_QUEUE, RabbitConst.DELAY_EXCHANGE, RabbitConst.RK_COUPON_EXPIRE);
    }

    @Bean
    public Binding deadFromDlxExchange() {
        return bind(RabbitConst.DEAD_QUEUE, RabbitConst.DLX_EXCHANGE, RabbitConst.RK_DEAD);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("com.comeoften.mq.message");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    /**
     * 死信队列专用容器工厂：不做 JSON 解析，只把原始字节交给监听器。
     *
     * <p>默认容器工厂用的是上面的 Jackson2JsonMessageConverter，遇到无法反序列化的毒消息时
     * 转换异常会发生在调用监听器之前，死信消费者根本没机会落库告警（实测日志
     * "Fatal and x-death present"）。死信的价值就是「留下证据」，所以这里换成
     * {@link SimpleMessageConverter}，并按全局配置继承 ack / prefetch。</p>
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rawRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(new SimpleMessageConverter());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    /**
     * Boot 2.7 未提供 RabbitTemplateCustomizer（Boot 3.x 才有）。
     * 用 SmartInitializingSingleton 在单例就绪、容器刷新完成前注册回调，避免 ApplicationRunner 的空窗期。
     */
    @Bean
    public SmartInitializingSingleton rabbitCallbackInitializer(RabbitTemplate rabbitTemplate,
                                                                RabbitConfirmCallback confirmCallback,
                                                                RabbitReturnsCallback returnsCallback) {
        return () -> {
            rabbitTemplate.setConfirmCallback(confirmCallback);
            rabbitTemplate.setReturnsCallback(returnsCallback);
        };
    }

    private static Queue businessQueue(String name) {
        return QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", RabbitConst.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitConst.RK_DEAD)
                .build();
    }

    private static Binding bind(String queue, String exchange, String routingKey) {
        return new Binding(queue, Binding.DestinationType.QUEUE, exchange, routingKey, null);
    }
}
