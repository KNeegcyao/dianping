package com.hmdp.config;

import com.hmdp.utils.RedisConstants;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient(){
        //配置
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379")
                .setConnectionPoolSize(4)
                .setConnectionMinimumIdleSize(1);
        // 使用 Netty 的堆内存分配，避免直接内存不足
        config.setNettyThreads(2);
        return Redisson.create(config);
    }
}
