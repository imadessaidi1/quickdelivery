package com.quickdelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Created by mac on 14/04/2020.
 */

@EnableConfigServer
@SpringBootApplication
@EnableDiscoveryClient
public class ConfigServerLauncher {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerLauncher.class, args);
    }
}
