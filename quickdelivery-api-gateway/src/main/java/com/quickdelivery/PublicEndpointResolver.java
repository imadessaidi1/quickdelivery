package com.quickdelivery;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

final class PublicEndpointResolver {

    private static final int FRONTEND_PORT = 8084;
    private static final int AUTH_PORT = 18443;
    private static final String REALM_PATH = "/auth/realms/quickdelivery";

    private PublicEndpointResolver() {
    }

    static Set<String> resolveFrontendOrigins(String configuredBaseUrls) {
        Set<String> origins = new LinkedHashSet<>();
        origins.addAll(normalizeBaseUrls(parseCsv(configuredBaseUrls)));
        addDefaultFrontendOrigins(origins);
        return origins;
    }

    private static void addDefaultFrontendOrigins(Set<String> origins) {
        origins.add("http://localhost");
        origins.add("https://localhost");
        origins.add("http://localhost:" + FRONTEND_PORT);
        origins.add("https://localhost:" + FRONTEND_PORT);
        for (String host : discoverLocalHosts()) {
            origins.add("http://" + host + ":" + FRONTEND_PORT);
            origins.add("https://" + host + ":" + FRONTEND_PORT);
        }
        origins.add("capacitor://localhost");
        origins.add("ionic://localhost");
    }

    static Set<String> resolveIssuerUris(String primaryIssuerUri, String additionalIssuerUris) {
        Set<String> issuers = new LinkedHashSet<>();
        boolean hasExplicitAdditionalIssuers = additionalIssuerUris != null && !additionalIssuerUris.isBlank();

        if (primaryIssuerUri != null && !primaryIssuerUri.isBlank()) {
            issuers.add(primaryIssuerUri.trim());
        }

        parseCsv(additionalIssuerUris).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .forEach(issuers::add);

        if (hasExplicitAdditionalIssuers) {
            return issuers;
        }

        if (issuers.isEmpty()) {
            issuers.add("https://localhost:" + AUTH_PORT + REALM_PATH);
        }
        for (String host : discoverLocalHosts()) {
            issuers.add("https://" + host + ":" + AUTH_PORT + REALM_PATH);
        }
        return issuers;
    }

    private static Set<String> normalizeBaseUrls(Set<String> rawBaseUrls) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String baseUrl : rawBaseUrls) {
            String value = baseUrl.trim();
            if (value.endsWith("/")) {
                value = value.substring(0, value.length() - 1);
            }
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private static Set<String> parseCsv(String csv) {
        Set<String> values = new LinkedHashSet<>();
        if (csv == null || csv.isBlank()) {
            return values;
        }

        Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .forEach(values::add);
        return values;
    }

    private static Set<String> discoverLocalHosts() {
        Set<String> hosts = new LinkedHashSet<>();
        addHost(hosts, "127.0.0.1");

        try {
            addHost(hosts, InetAddress.getLocalHost().getHostName());
        } catch (Exception ignored) {
        }

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address) {
                        addHost(hosts, address.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {
        }

        return hosts;
    }

    private static void addHost(Set<String> hosts, String host) {
        if (host == null) {
            return;
        }
        String normalized = host.trim();
        if (!normalized.isBlank()) {
            hosts.add(normalized);
        }
    }
}
