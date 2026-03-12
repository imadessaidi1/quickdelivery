package com.quickdelivery;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by mac on 16/04/2020.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class APIGatewayApplication {

    public static void main(String[] args) {
        configureLocalTrustStore();
        SpringApplication.run(APIGatewayApplication.class, args);
    }

    private static void configureLocalTrustStore() {
        setSystemPropertyIfBlank("javax.net.ssl.trustStoreType",
                System.getenv().getOrDefault("API_GATEWAY_SSL_TRUST_STORE_TYPE", "PKCS12"));

        if (System.getProperty("javax.net.ssl.trustStore") == null || System.getProperty("javax.net.ssl.trustStore").isBlank()) {
            Path trustStorePath = resolveTrustStorePath();
            if (trustStorePath != null) {
                System.setProperty("javax.net.ssl.trustStore", trustStorePath.toAbsolutePath().normalize().toString());
            }
        }

        setSystemPropertyIfBlank(
                "javax.net.ssl.trustStorePassword",
                System.getenv().getOrDefault("API_GATEWAY_SSL_TRUST_STORE_PASSWORD", "QuickDelivery123@")
        );

        System.out.println(
                "Gateway truststore: path=" + System.getProperty("javax.net.ssl.trustStore")
                        + ", type=" + System.getProperty("javax.net.ssl.trustStoreType")
        );
    }

    private static Path resolveTrustStorePath() {
        String configuredPath = System.getenv().getOrDefault("API_GATEWAY_SSL_TRUST_STORE", "./certs/quickdelivery-dev.p12");
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

    /*@Autowired
    private TokenRelayGatewayFilterFactory filterFactory;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("IAMService", r -> r.path("/IAM/v1/**")
                        .filters(f -> f.filters(filterFactory.apply())
                                .removeRequestHeader("Cookie")) // Prevents cookie being sent downstream
                        .uri("lb://IAM-SEREVICES")) // Taking advantage of docker naming
                .build();
    }*/

    @Bean
    public ReactiveResilience4JCircuitBreakerFactory reactiveResilience4JCircuitBreakerFactory(
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry
    ) {
        ReactiveResilience4JCircuitBreakerFactory reactiveResilience4JCircuitBreakerFactory =
                new ReactiveResilience4JCircuitBreakerFactory(circuitBreakerRegistry, timeLimiterRegistry);
        reactiveResilience4JCircuitBreakerFactory.configureCircuitBreakerRegistry(circuitBreakerRegistry);
        return reactiveResilience4JCircuitBreakerFactory;
    }

    @EventListener
    public void handleContextRefreshed(ContextRefreshedEvent event) {
        printActiveProperties((ConfigurableEnvironment) event.getApplicationContext().getEnvironment());
    }

    private void printActiveProperties(ConfigurableEnvironment env) {

        System.out.println("************************* ACTIVE APP PROPERTIES ******************************");

        List<MapPropertySource> propertySources = new ArrayList<>();

        env.getPropertySources().forEach(it -> {
            if (it instanceof MapPropertySource && it.getName().contains("applicationConfig")) {
                propertySources.add((MapPropertySource) it);
            }
        });

        propertySources.stream()
                .map(propertySource -> propertySource.getSource().keySet())
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .forEach(key -> {
                    try {
                        System.out.println(key + "=" + env.getProperty(key));
                    } catch (Exception e) {
                        System.out.println(key +" "+e.getMessage());
                    }
                });
        System.out.println("******************************************************************************");
    }
}
