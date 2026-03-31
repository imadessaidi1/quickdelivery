package com.quickdelivery;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class GatewayCacheConfig {

    @Value("${quickdelivery.gateway.metrics.cache-ttl-seconds:20}")
    private long metricsCacheTtlSeconds;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.registerCustomCache("gatewayAdminMetrics",
                Caffeine.newBuilder().maximumSize(32).expireAfterWrite(metricsCacheTtlSeconds, TimeUnit.SECONDS).build());
        cacheManager.registerCustomCache("gatewayAdminLogInsights",
                Caffeine.newBuilder().maximumSize(32).expireAfterWrite(metricsCacheTtlSeconds, TimeUnit.SECONDS).build());
        cacheManager.registerCustomCache("gatewayAdminHttpBreakdown",
                Caffeine.newBuilder().maximumSize(32).expireAfterWrite(metricsCacheTtlSeconds, TimeUnit.SECONDS).build());
        cacheManager.registerCustomCache("gatewayUsersAdminMetrics",
                Caffeine.newBuilder().maximumSize(32).expireAfterWrite(metricsCacheTtlSeconds, TimeUnit.SECONDS).build());
        cacheManager.registerCustomCache("gatewayUsersAdminHttpBreakdown",
                Caffeine.newBuilder().maximumSize(32).expireAfterWrite(metricsCacheTtlSeconds, TimeUnit.SECONDS).build());
        cacheManager.registerCustomCache("gatewayUsersAdminLogInsights",
                Caffeine.newBuilder().maximumSize(32).expireAfterWrite(metricsCacheTtlSeconds, TimeUnit.SECONDS).build());
        cacheManager.registerCustomCache("gatewayPackagesAdminMetrics",
                Caffeine.newBuilder().maximumSize(32).expireAfterWrite(metricsCacheTtlSeconds, TimeUnit.SECONDS).build());
        cacheManager.registerCustomCache("gatewayPackagesAdminHttpBreakdown",
                Caffeine.newBuilder().maximumSize(32).expireAfterWrite(metricsCacheTtlSeconds, TimeUnit.SECONDS).build());
        cacheManager.registerCustomCache("gatewayPackagesAdminLogInsights",
                Caffeine.newBuilder().maximumSize(32).expireAfterWrite(metricsCacheTtlSeconds, TimeUnit.SECONDS).build());
        cacheManager.registerCustomCache("gatewayTrackingAdminMetrics",
                Caffeine.newBuilder().maximumSize(32).expireAfterWrite(metricsCacheTtlSeconds, TimeUnit.SECONDS).build());
        cacheManager.registerCustomCache("gatewayConfigAdminMetrics",
                Caffeine.newBuilder().maximumSize(32).expireAfterWrite(metricsCacheTtlSeconds, TimeUnit.SECONDS).build());
        cacheManager.registerCustomCache("gatewayDiscoveryAdminMetrics",
                Caffeine.newBuilder().maximumSize(32).expireAfterWrite(metricsCacheTtlSeconds, TimeUnit.SECONDS).build());
        cacheManager.registerCustomCache("gatewayOauthAdminMetrics",
                Caffeine.newBuilder().maximumSize(32).expireAfterWrite(metricsCacheTtlSeconds, TimeUnit.SECONDS).build());
        cacheManager.registerCustomCache("gatewayRedisAdminMetrics",
                Caffeine.newBuilder().maximumSize(32).expireAfterWrite(metricsCacheTtlSeconds, TimeUnit.SECONDS).build());
        cacheManager.registerCustomCache("gatewayMysqlAdminMetrics",
                Caffeine.newBuilder().maximumSize(32).expireAfterWrite(metricsCacheTtlSeconds, TimeUnit.SECONDS).build());
        return cacheManager;
    }
}
