package com.quickdelivery.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {
    @Value("${quickdelivery.cache.ttl-seconds:15}")
    private long defaultCacheTtlSeconds;
    @Value("${quickdelivery.cache.hot-ttl-seconds:5}")
    private long hotCacheTtlSeconds;
    @Value("${quickdelivery.cache.nearby-ttl-seconds:10}")
    private long nearbyCacheTtlSeconds;
    @Value("${quickdelivery.cache.dashboard-ttl-seconds:20}")
    private long dashboardCacheTtlSeconds;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.registerCustomCache("packagesAroundGrouped", buildCache("packagesAroundGrouped", nearbyCacheTtlSeconds, 512));
        cacheManager.registerCustomCache("packagesAroundMe", buildCache("packagesAroundMe", nearbyCacheTtlSeconds, 512));
        cacheManager.registerCustomCache("packagesAroundDestination", buildCache("packagesAroundDestination", nearbyCacheTtlSeconds, 512));
        cacheManager.registerCustomCache("packagesByStatus", buildCache("packagesByStatus", hotCacheTtlSeconds, 256));
        cacheManager.registerCustomCache("packagesByDeliveryPerson", buildCache("packagesByDeliveryPerson", hotCacheTtlSeconds, 512));
        cacheManager.registerCustomCache("packagesBySender", buildCache("packagesBySender", hotCacheTtlSeconds, 512));
        cacheManager.registerCustomCache("userWithOngoingDelivery", buildCache("userWithOngoingDelivery", hotCacheTtlSeconds, 512));
        cacheManager.registerCustomCache("packagesAdminMetrics", buildCache("packagesAdminMetrics", dashboardCacheTtlSeconds, 64));
        cacheManager.registerCustomCache("packagesAdminHttpBreakdown", buildCache("packagesAdminHttpBreakdown", dashboardCacheTtlSeconds, 64));
        cacheManager.registerCustomCache("packagesAdminFinancialDashboard", buildCache("packagesAdminFinancialDashboard", dashboardCacheTtlSeconds, 64));
        cacheManager.registerCustomCache("packagesAdminDashboardSummary", buildCache("packagesAdminDashboardSummary", dashboardCacheTtlSeconds, 64));
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(32)
                .maximumSize(1_000)
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

    @Bean("customKeyGenerator")
    public KeyGenerator keyGenerator() {
        return new CustomKeyGenerator();
    }
}

