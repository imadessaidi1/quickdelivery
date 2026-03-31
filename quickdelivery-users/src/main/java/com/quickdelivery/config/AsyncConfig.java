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
    @Value("${quickdelivery.async.user.core-pool-size:4}")
    private int userCorePoolSize;
    @Value("${quickdelivery.async.user.max-pool-size:8}")
    private int userMaxPoolSize;
    @Value("${quickdelivery.async.user.queue-capacity:150}")
    private int userQueueCapacity;
    @Value("${quickdelivery.async.mail.core-pool-size:2}")
    private int mailCorePoolSize;
    @Value("${quickdelivery.async.mail.max-pool-size:4}")
    private int mailMaxPoolSize;
    @Value("${quickdelivery.async.mail.queue-capacity:100}")
    private int mailQueueCapacity;
    @Value("${quickdelivery.async.ocr.core-pool-size:2}")
    private int ocrCorePoolSize;
    @Value("${quickdelivery.async.ocr.max-pool-size:4}")
    private int ocrMaxPoolSize;
    @Value("${quickdelivery.async.ocr.queue-capacity:100}")
    private int ocrQueueCapacity;

    @Bean(name = "userAsyncTaskExecutor")
    public ThreadPoolTaskExecutor userAsyncTaskExecutor() {
        return buildExecutor(userCorePoolSize, userMaxPoolSize, userQueueCapacity, "user-async-");
    }

    @Bean(name = "mailTaskExecutor")
    public ThreadPoolTaskExecutor mailTaskExecutor() {
        return buildExecutor(mailCorePoolSize, mailMaxPoolSize, mailQueueCapacity, "mail-");
    }

    @Bean(name = "ocrTaskExecutor")
    public ThreadPoolTaskExecutor ocrTaskExecutor() {
        return buildExecutor(ocrCorePoolSize, ocrMaxPoolSize, ocrQueueCapacity, "ocr-");
    }

    private ThreadPoolTaskExecutor buildExecutor(int corePoolSize, int maxPoolSize, int queueCapacity, String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
