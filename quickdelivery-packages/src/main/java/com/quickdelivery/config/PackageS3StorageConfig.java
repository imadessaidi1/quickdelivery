package com.quickdelivery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class PackageS3StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "quickdelivery.package-docs.mode", havingValue = "s3")
    public S3Client packageS3Client(@Value("${AWS_REGION:eu-west-3}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
