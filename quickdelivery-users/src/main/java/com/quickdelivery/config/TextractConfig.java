package com.quickdelivery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;

@Configuration
public class TextractConfig {
    @Bean
    @ConditionalOnProperty(name = "quickdelivery.ocr.textract.enabled", havingValue = "true")
    public TextractClient textractClient(@Value("${quickdelivery.ocr.textract.region:eu-west-3}") String region) {
        return TextractClient.builder()
                .region(Region.of(region))
                .build();
    }
}
