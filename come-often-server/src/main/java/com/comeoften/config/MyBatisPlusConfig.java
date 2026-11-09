package com.comeoften.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 分页插件与雪花 ID 生成器。
 * 多实例部署时用 comeoften.snowflake.* 环境变量注入不同的 workerId/datacenterId。
 */
@Configuration
public class MyBatisPlusConfig {

    private static final int MAX_NODE_ID = 31;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public IdentifierGenerator identifierGenerator(
            @Value("${comeoften.snowflake.worker-id:0}") int workerId,
            @Value("${comeoften.snowflake.datacenter-id:0}") int datacenterId) {
        if (workerId < 0 || workerId > MAX_NODE_ID || datacenterId < 0 || datacenterId > MAX_NODE_ID) {
            throw new IllegalStateException("雪花算法 workerId/datacenterId 必须在 0~31 之间");
        }
        return new DefaultIdentifierGenerator(workerId, datacenterId);
    }
}
