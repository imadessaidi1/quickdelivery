package com.quickdelivery;

import com.google.maps.GeoApiContext;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${googlemapsapi.key}")
    private String googleMapsApiKey;
        @Bean
        @Scope("prototype")
        public ModelMapper modelMapper() {
            return new ModelMapper();
        }

    @Bean
    @Scope("prototype")
    public GeoApiContext iniiateGoogleGeoCoder(){
        GeoApiContext context = new GeoApiContext.Builder()
            .apiKey(googleMapsApiKey)
            .build();
        return context;
    }
    public static void main(String[] args) {
            SpringApplication.run(PackageMain.class, args);
        }
}