package com.quickdelivery;

import org.springframework.cloud.netflix.eureka.http.EurekaClientHttpRequestFactorySupplier;
import org.springframework.cloud.netflix.eureka.http.RestTemplateDiscoveryClientOptionalArgs;
import org.springframework.cloud.netflix.eureka.http.RestTemplateTransportClientFactories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestTemplateConfiguration {
    @Bean
    public RestTemplateTransportClientFactories restTemplateTransportClientFactories(EurekaClientHttpRequestFactorySupplier supplier)
    {
        return new RestTemplateTransportClientFactories(new RestTemplateDiscoveryClientOptionalArgs(supplier));
    }
}
