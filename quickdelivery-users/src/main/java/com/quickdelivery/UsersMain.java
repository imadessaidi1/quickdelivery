package com.quickdelivery;

import com.google.maps.GeoApiContext;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@ComponentScan("com.quickdelivery")
@EnableTransactionManagement
public class UsersMain {
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
    @Bean
    public Logger initLogger(){
        return LoggerFactory.getLogger(this.getClass());
    }
    public static void main(String[] args) {
        configureLocalTrustStore();
        SpringApplication.run(UsersMain.class, args);
    }

    private static void configureLocalTrustStore() {
        setSystemPropertyIfBlank(
                "javax.net.ssl.trustStoreType",
                System.getenv().getOrDefault("USERS_SSL_TRUST_STORE_TYPE", "PKCS12")
        );

        if (System.getProperty("javax.net.ssl.trustStore") == null || System.getProperty("javax.net.ssl.trustStore").isBlank()) {
            Path trustStorePath = resolveTrustStorePath();
            if (trustStorePath != null) {
                System.setProperty("javax.net.ssl.trustStore", trustStorePath.toAbsolutePath().normalize().toString());
            }
        }

        setSystemPropertyIfBlank(
                "javax.net.ssl.trustStorePassword",
                System.getenv().getOrDefault("USERS_SSL_TRUST_STORE_PASSWORD", "QuickDelivery123@")
        );

        System.out.println(
                "Users truststore: path=" + System.getProperty("javax.net.ssl.trustStore")
                        + ", type=" + System.getProperty("javax.net.ssl.trustStoreType")
        );
    }

    private static Path resolveTrustStorePath() {
        String configuredPath = System.getenv().getOrDefault("USERS_SSL_TRUST_STORE", "./certs/quickdelivery-dev.p12");
        String sanitizedPath = configuredPath.replaceFirst("^file:", "");
        Path candidatePath = Paths.get(sanitizedPath);

        if (Files.exists(candidatePath)) {
            return candidatePath;
        }

        Path fallbackPath = Paths.get("certs", "quickdelivery-dev.p12");
        if (Files.exists(fallbackPath)) {
            return fallbackPath;
        }

        return null;
    }

    private static void setSystemPropertyIfBlank(String key, String value) {
        String existingValue = System.getProperty(key);
        if (existingValue == null || existingValue.isBlank()) {
            System.setProperty(key, value);
        }
    }
}
