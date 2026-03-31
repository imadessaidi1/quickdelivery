package com.quickdelivery.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
    @Value("${quickdelivery.cache.ttl-seconds:15}")
    private long defaultCacheTtlSeconds;
    @Value("${quickdelivery.cache.validation-ttl-seconds:10}")
    private long validationCacheTtlSeconds;
    @Value("${quickdelivery.cache.dashboard-ttl-seconds:20}")
    private long dashboardCacheTtlSeconds;
    @Value("${quickdelivery.cache.admin-overview-ttl-seconds:60}")
    private long adminOverviewCacheTtlSeconds;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.registerCustomCache("usersValidationPage", buildCache("usersValidationPage", validationCacheTtlSeconds, 128));
        cacheManager.registerCustomCache("usersAdminMetrics", buildCache("usersAdminMetrics", dashboardCacheTtlSeconds, 64));
        cacheManager.registerCustomCache("usersAdminUserOverview", buildCache("usersAdminUserOverview", adminOverviewCacheTtlSeconds, 32));
        cacheManager.registerCustomCache("usersAdminHttpBreakdown", buildCache("usersAdminHttpBreakdown", dashboardCacheTtlSeconds, 64));
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(16)
                .maximumSize(512)
                .expireAfterWrite(defaultCacheTtlSeconds, TimeUnit.SECONDS));
        return cacheManager;
    }

    private com.github.benmanes.caffeine.cache.Cache<Object, Object> buildCache(String name, long ttlSeconds, long maxSize) {
        return Caffeine.newBuilder()
                .initialCapacity(16)
                .maximumSize(maxSize)
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .build();
    }
}
