package com.jonathanfoucher.redislockexample.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.integration.support.locks.ExpirableLockRegistry;

import java.time.Duration;

@Configuration
public class RedisConfig {
    @Value("${redis-lock-example.lock-registry-key}")
    private String lockRegistryKey;
    @Value("${redis-lock-example.release-time-duration}")
    private int releaseTimeDuration;

    @Bean
    public ExpirableLockRegistry lockRegistry(RedisConnectionFactory redisConnectionFactory) {
        return new RedisLockRegistry(
                redisConnectionFactory,
                lockRegistryKey,
                Duration.ofSeconds(releaseTimeDuration).toMillis()
        );
    }
}
