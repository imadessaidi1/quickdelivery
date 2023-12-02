package com.quickdelivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Created by mac on 15/04/2020.
 */

@EnableEurekaServer
@SpringBootApplication
public class ServiceRegistrationAndDiscoveryServer {
    public static void main(String[] args) {
        SpringApplication.run(ServiceRegistrationAndDiscoveryServer.class, args);
    }

}
