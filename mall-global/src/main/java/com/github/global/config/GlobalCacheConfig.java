package com.github.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@EnableCaching
@Configuration
public class GlobalCacheConfig {

    @Bean("cacheManager1m")
    public CacheManager cacheManager1m() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder().maximumSize(1000);
        caffeine.expireAfterWrite(1, TimeUnit.MINUTES);
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }

    @Primary
    @Bean("defaultCacheManager")
    public CacheManager defaultCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder().maximumSize(5000);
        caffeine.expireAfterWrite(5, TimeUnit.MINUTES);
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }

    @Bean("cacheManager15m")
    public CacheManager cacheManager15m() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder().maximumSize(15000);
        caffeine.expireAfterWrite(15, TimeUnit.MINUTES);
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }
}
