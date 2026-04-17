package com.quickdelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;

@SpringBootApplication
@ComponentScan("com.quickdelivery")
@EnableTransactionManagement
@EnableScheduling
@EnableDiscoveryClient
public class ShipMain {

    public static void main(String[] args) {
        configureLocalTrustStore();
        SpringApplication.run(ShipMain.class, args);
    }

    private static void configureLocalTrustStore() {
        String configuredPath = System.getenv().getOrDefault("SHIPMENT_SSL_TRUST_STORE", "./certs/quickdelivery-dev.p12");
        Path candidatePath = Paths.get(configuredPath.replaceFirst("^file:", ""));
        Path fallbackPath = Paths.get("certs", "quickdelivery-dev.p12");

        Path trustStorePath = Files.exists(candidatePath) ? candidatePath
                : (Files.exists(fallbackPath) ? fallbackPath : null);

        if (trustStorePath == null) {
            System.out.println("Shipment truststore: no additional dev truststore found");
            return;
        }

        try {
            String password = System.getenv().getOrDefault("SHIPMENT_SSL_TRUST_STORE_PASSWORD", "QuickDelivery123@");
            Path mergedPath = buildMergedTrustStore("shipment-truststore", trustStorePath, "PKCS12", password);
            System.setProperty("javax.net.ssl.trustStore", mergedPath.toAbsolutePath().normalize().toString());
            System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
            System.setProperty("javax.net.ssl.trustStorePassword", password);
            System.out.println("Shipment truststore: " + mergedPath.toAbsolutePath().normalize());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to configure shipment SSL truststore", e);
        }
    }

    private static Path buildMergedTrustStore(String prefix, Path localTrustStorePath, String type, String password) throws Exception {
        KeyStore local = KeyStore.getInstance(type);
        try (var in = Files.newInputStream(localTrustStorePath)) {
            local.load(in, password.toCharArray());
        }

        Path cacertsPath = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
        KeyStore merged = KeyStore.getInstance("PKCS12");
        merged.load(null, password.toCharArray());

        if (Files.exists(cacertsPath)) {
            try {
                KeyStore cacerts = KeyStore.getInstance("PKCS12");
                try (var in = Files.newInputStream(cacertsPath)) {
                    cacerts.load(in, "changeit".toCharArray());
                }
                var aliases = cacerts.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    Certificate cert = cacerts.getCertificate(alias);
                    if (cert != null) merged.setCertificateEntry(alias, cert);
                }
            } catch (Exception ignored) {
            }
        }

        var aliases = local.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            Certificate cert = local.getCertificate(alias);
            if (cert != null) merged.setCertificateEntry("quickdelivery-" + alias, cert);
        }

        Path out = Files.createTempFile(prefix, ".p12");
        try (var os = Files.newOutputStream(out)) {
            merged.store(os, password.toCharArray());
        }
        out.toFile().deleteOnExit();
        return out;
    }
}
