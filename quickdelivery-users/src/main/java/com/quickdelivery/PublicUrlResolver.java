package com.quickdelivery;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public final class PublicUrlResolver {

    private static final int FRONTEND_PORT = 8084;
    private static final int GATEWAY_PORT = 8443;

    private PublicUrlResolver() {
    }

    public static String resolvePreferredFrontendBaseUrl(String configuredBaseUrl) {
        return resolvePreferredBaseUrl(configuredBaseUrl, FRONTEND_PORT);
    }

    public static String resolvePreferredGatewayBaseUrl(String configuredBaseUrl) {
        return resolvePreferredBaseUrl(configuredBaseUrl, GATEWAY_PORT);
    }

    private static String resolvePreferredBaseUrl(String configuredBaseUrl, int defaultPort) {
        if (configuredBaseUrl != null && !configuredBaseUrl.isBlank()) {
            return normalize(configuredBaseUrl);
        }

        String discoveredHost = discoverPreferredHost();
        return "https://" + discoveredHost + ":" + defaultPort;
    }

    private static String discoverPreferredHost() {
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
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {
        }

        return "localhost";
    }

    private static String normalize(String baseUrl) {
        String value = baseUrl.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
