package com.quickdelivery;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
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
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by mac on 16/04/2020.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
public class APIGatewayApplication {

    public static void main(String[] args) {
        configureLocalTrustStore();
        SpringApplication.run(APIGatewayApplication.class, args);
    }

    private static void configureLocalTrustStore() {
        Path trustStorePath = resolveTrustStorePath();
        String trustStoreType = System.getenv().getOrDefault("API_GATEWAY_SSL_TRUST_STORE_TYPE", "PKCS12");
        String trustStorePassword = System.getenv().getOrDefault("API_GATEWAY_SSL_TRUST_STORE_PASSWORD", "QuickDelivery123@");

        if (trustStorePath == null) {
            System.out.println("Gateway truststore: no additional dev truststore found");
            return;
        }

        try {
            Path mergedTrustStorePath = buildMergedTrustStore(
                    "api-gateway-truststore",
                    trustStorePath,
                    trustStoreType,
                    trustStorePassword
            );

            System.setProperty("javax.net.ssl.trustStore", mergedTrustStorePath.toAbsolutePath().normalize().toString());
            System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);

            System.out.println(
                    "Gateway truststore: path=" + mergedTrustStorePath.toAbsolutePath().normalize()
                            + ", type=PKCS12, mode=merged-default"
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to configure gateway SSL truststore", exception);
        }
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

    private static Path buildMergedTrustStore(String prefix, Path localTrustStorePath, String localTrustStoreType, String password) throws Exception {
        KeyStore mergedKeyStore = loadDefaultCacerts(password);
        KeyStore localKeyStore = KeyStore.getInstance(localTrustStoreType);
        try (var inputStream = Files.newInputStream(localTrustStorePath)) {
            localKeyStore.load(inputStream, password.toCharArray());
        }

        var aliases = localKeyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate certificate = localKeyStore.getCertificate(alias);
            if (certificate != null) {
                mergedKeyStore.setCertificateEntry("quickdelivery-" + alias, certificate);
            }
        }

        Path mergedPath = Files.createTempFile(prefix, ".p12");
        try (var outputStream = Files.newOutputStream(mergedPath)) {
            mergedKeyStore.store(outputStream, password.toCharArray());
        }
        mergedPath.toFile().deleteOnExit();
        return mergedPath;
    }

    private static KeyStore loadDefaultCacerts(String password) throws Exception {
        Path cacertsPath = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (var inputStream = Files.newInputStream(cacertsPath)) {
            try {
                keyStore.load(inputStream, "changeit".toCharArray());
                return keyStore;
            } catch (Exception ignored) {
            }
        }

        keyStore = KeyStore.getInstance("JKS");
        try (var inputStream = Files.newInputStream(cacertsPath)) {
            keyStore.load(inputStream, "changeit".toCharArray());
        }

        KeyStore pkcs12KeyStore = KeyStore.getInstance("PKCS12");
        pkcs12KeyStore.load(null, password.toCharArray());
        var aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate certificate = keyStore.getCertificate(alias);
            if (certificate != null) {
                pkcs12KeyStore.setCertificateEntry(alias, certificate);
            }
        }
        return pkcs12KeyStore;
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
