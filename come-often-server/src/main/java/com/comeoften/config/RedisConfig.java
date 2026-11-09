package com.comeoften.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * Redis 脚本注册，禁止在 Java 里手拼 Lua 命令。
 */
@Configuration
public class RedisConfig {

    @Bean
    public DefaultRedisScript<Long> stockDeductScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/stock_deduct.lua")));
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 任务锁释放脚本：比对 token 后再删除，GET 与 DEL 之间不会插进别人的 SETNX。
     */
    @Bean
    public DefaultRedisScript<Long> jobLockReleaseScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/job_lock_release.lua")));
        script.setResultType(Long.class);
        return script;
    }
}
