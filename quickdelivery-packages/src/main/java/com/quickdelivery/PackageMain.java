package com.quickdelivery;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ComponentScan("com.quickdelivery")
@EnableTransactionManagement
public class PackageMain {
        @Bean
        @Scope("prototype")
        public ModelMapper modelMapper() {
            return new ModelMapper();
        }
        public static void main(String[] args) {
            SpringApplication.run(PackageMain.class, args);
        }
}