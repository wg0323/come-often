package com.comeoften;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 私域餐饮小程序后端启动类。M3 起启用定时补偿任务（本地消息表未确认重发）。
 */
@SpringBootApplication
@EnableScheduling
@MapperScan({"com.comeoften.module.*.mapper", "com.comeoften.mq.mapper"})
public class ComeOftenApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComeOftenApplication.class, args);
    }
}
