package com.example.Assesment_two.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class CacheConfig {

    @Bean
    public Cache<Object, Object> caffeineCache(
            @Value("${weather.cache.maximum-size:500}") int maximumSize,
            @Value("${weather.cache.expire-after-write-minutes:30}") int expireMinutes) {
        log.info("Configured Caffeine cache: maxSize={}, expireAfterWrite={}min", maximumSize, expireMinutes);
        return Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(Duration.ofMinutes(expireMinutes))
                .recordStats()
                .build();
    }

    @Bean
    public CacheManager cacheManager(Cache<Object, Object> caffeineCache) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("weather-cache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(30))
                .recordStats());
        return cacheManager;
    }
}
