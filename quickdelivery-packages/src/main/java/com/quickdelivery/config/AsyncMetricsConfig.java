package com.quickdelivery.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncMetricsConfig {

    public AsyncMetricsConfig(MeterRegistry meterRegistry,
                              @Qualifier("packageAsyncTaskExecutor") ThreadPoolTaskExecutor packageAsyncTaskExecutor,
                              @Qualifier("packageMailTaskExecutor") ThreadPoolTaskExecutor packageMailTaskExecutor) {
        registerExecutorMetrics(meterRegistry, "packageAsyncTaskExecutor", packageAsyncTaskExecutor);
        registerExecutorMetrics(meterRegistry, "packageMailTaskExecutor", packageMailTaskExecutor);
    }

    private void registerExecutorMetrics(MeterRegistry meterRegistry, String executorName, ThreadPoolTaskExecutor executor) {
        Gauge.builder("quickdelivery.async.queue.size", executor, value -> value.getThreadPoolExecutor().getQueue().size())
                .tag("executor", executorName)
                .register(meterRegistry);
        Gauge.builder("quickdelivery.async.active.count", executor, value -> value.getActiveCount())
                .tag("executor", executorName)
                .register(meterRegistry);
        Gauge.builder("quickdelivery.async.pool.size", executor, value -> value.getPoolSize())
                .tag("executor", executorName)
                .register(meterRegistry);
    }
}
