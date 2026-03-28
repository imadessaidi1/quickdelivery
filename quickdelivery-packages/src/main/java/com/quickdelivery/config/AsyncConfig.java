package com.quickdelivery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Value("${quickdelivery.async.package.core-pool-size:2}")
    private int packageCorePoolSize;
    @Value("${quickdelivery.async.package.max-pool-size:4}")
    private int packageMaxPoolSize;
    @Value("${quickdelivery.async.package.queue-capacity:100}")
    private int packageQueueCapacity;
    @Value("${quickdelivery.async.mail.core-pool-size:2}")
    private int mailCorePoolSize;
    @Value("${quickdelivery.async.mail.max-pool-size:4}")
    private int mailMaxPoolSize;
    @Value("${quickdelivery.async.mail.queue-capacity:100}")
    private int mailQueueCapacity;

    @Bean(name = "packageAsyncTaskExecutor")
    public ThreadPoolTaskExecutor packageAsyncTaskExecutor() {
        return buildExecutor(packageCorePoolSize, packageMaxPoolSize, packageQueueCapacity, "package-async-");
    }

    @Bean(name = "packageMailTaskExecutor")
    public ThreadPoolTaskExecutor packageMailTaskExecutor() {
        return buildExecutor(mailCorePoolSize, mailMaxPoolSize, mailQueueCapacity, "package-mail-");
    }

    private ThreadPoolTaskExecutor buildExecutor(int corePoolSize, int maxPoolSize, int queueCapacity, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
