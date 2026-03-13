package com.quickdelivery;

import com.google.maps.GeoApiContext;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;

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
    @Bean
    public Logger initLogger(){
            return LoggerFactory.getLogger(this.getClass());
    }
    public static void main(String[] args) {
            configureLocalTrustStore();
            SpringApplication.run(PackageMain.class, args);
        }

    private static void configureLocalTrustStore() {
        Path trustStorePath = resolveTrustStorePath();
        String trustStoreType = System.getenv().getOrDefault("PACKAGES_SSL_TRUST_STORE_TYPE", "PKCS12");
        String trustStorePassword = System.getenv().getOrDefault("PACKAGES_SSL_TRUST_STORE_PASSWORD", "QuickDelivery123@");

        if (trustStorePath == null) {
            System.out.println("Packages truststore: no additional dev truststore found");
            return;
        }

        try {
            Path mergedTrustStorePath = buildMergedTrustStore(
                    "packages-truststore",
                    trustStorePath,
                    trustStoreType,
                    trustStorePassword
            );

            System.setProperty("javax.net.ssl.trustStore", mergedTrustStorePath.toAbsolutePath().normalize().toString());
            System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);

            System.out.println(
                    "Packages truststore: path=" + mergedTrustStorePath.toAbsolutePath().normalize()
                            + ", type=PKCS12, mode=merged-default"
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to configure packages SSL truststore", exception);
        }
    }

    private static Path resolveTrustStorePath() {
        String configuredPath = System.getenv().getOrDefault("PACKAGES_SSL_TRUST_STORE", "./certs/quickdelivery-dev.p12");
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
}
