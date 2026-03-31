package com.quickdelivery;

import com.quickdelivery.dto.GatewayHourlyLogCountResponse;
import com.quickdelivery.dto.GatewayHttpEndpointMetricResponse;
import com.quickdelivery.dto.GatewayLogEventResponse;
import com.quickdelivery.dto.GatewayServiceAggregateResponse;
import com.quickdelivery.dto.GatewayServiceHttpBreakdownResponse;
import com.quickdelivery.dto.GatewayServiceInstanceResponse;
import com.quickdelivery.dto.GatewayServiceLogInsightsResponse;
import com.quickdelivery.dto.GatewayServiceMetricsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.Socket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class GatewayServiceAggregationService {

    private final DiscoveryClient discoveryClient;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${quickdelivery.gateway.internal-token}")
    private String gatewayInternalToken;

    @Value("${quickdelivery.gateway.metrics.instance-timeout-millis:3000}")
    private long instanceTimeoutMillis;

    @Value("${quickdelivery.config-server.base-url:}")
    private String configServerBaseUrl;
    @Value("${quickdelivery.discovery-server.base-url:}")
    private String discoveryServerBaseUrl;
    @Value("${quickdelivery.oauth-server.base-url:}")
    private String oauthServerBaseUrl;
    @Value("${spring.cloud.config.username:}")
    private String configServerUsername;
    @Value("${spring.cloud.config.password:}")
    private String configServerPassword;
    @Value("${quickdelivery.redis.host:}")
    private String redisHost;
    @Value("${quickdelivery.redis.port:6379}")
    private int redisPort;
    @Value("${quickdelivery.mysql.host:}")
    private String mysqlHost;
    @Value("${quickdelivery.mysql.port:3306}")
    private int mysqlPort;

    public GatewayServiceAggregationService(DiscoveryClient discoveryClient, ObjectMapper objectMapper) {
        this.discoveryClient = discoveryClient;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> aggregateMetrics(String serviceId, String displayName, String endpointPath) {
        List<GatewayServiceInstanceResponse<GatewayServiceMetricsResponse>> instances =
                fetchInstances(serviceId, endpointPath, GatewayServiceMetricsResponse.class);
        GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> response = new GatewayServiceAggregateResponse<>();
        response.setServiceName(displayName);
        response.setInstances(instances);
        response.setTotalInstances(instances.size());
        response.setHealthyInstances((int) instances.stream().filter(instance -> Boolean.TRUE.equals(instance.getAvailable())).count());
        response.setSummary(aggregateMetricsSummary(displayName, instances));
        return response;
    }

    public GatewayServiceAggregateResponse<GatewayServiceHttpBreakdownResponse> aggregateHttpBreakdown(String serviceId, String displayName, String endpointPath) {
        List<GatewayServiceInstanceResponse<GatewayServiceHttpBreakdownResponse>> instances =
                fetchInstances(serviceId, endpointPath, GatewayServiceHttpBreakdownResponse.class);
        GatewayServiceAggregateResponse<GatewayServiceHttpBreakdownResponse> response = new GatewayServiceAggregateResponse<>();
        response.setServiceName(displayName);
        response.setInstances(instances);
        response.setTotalInstances(instances.size());
        response.setHealthyInstances((int) instances.stream().filter(instance -> Boolean.TRUE.equals(instance.getAvailable())).count());
        response.setSummary(aggregateHttpSummary(displayName, instances));
        return response;
    }

    public GatewayServiceAggregateResponse<GatewayServiceLogInsightsResponse> aggregateLogInsights(String serviceId, String displayName, String endpointPath) {
        List<GatewayServiceInstanceResponse<GatewayServiceLogInsightsResponse>> instances =
                fetchInstances(serviceId, endpointPath, GatewayServiceLogInsightsResponse.class);
        GatewayServiceAggregateResponse<GatewayServiceLogInsightsResponse> response = new GatewayServiceAggregateResponse<>();
        response.setServiceName(displayName);
        response.setInstances(instances);
        response.setTotalInstances(instances.size());
        response.setHealthyInstances((int) instances.stream().filter(instance -> Boolean.TRUE.equals(instance.getAvailable())).count());
        response.setSummary(aggregateLogSummary(displayName, instances));
        return response;
    }

    public GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> aggregateSingletonMetrics(String displayName, String instanceId, String baseUrl) {
        GatewayServiceInstanceResponse<GatewayServiceMetricsResponse> instance = new GatewayServiceInstanceResponse<>();
        instance.setInstanceId(instanceId);
        instance.setHost(extractHost(baseUrl));
        instance.setIpAddress(extractHost(baseUrl));
        instance.setPort(extractPort(baseUrl));
        instance.setSecure(baseUrl != null && baseUrl.startsWith("https://"));
        instance.setAvailable(false);

        if (baseUrl != null && !baseUrl.isBlank()) {
            try {
                GatewayServiceMetricsResponse payload = fetchSingletonMetrics(displayName, baseUrl, requiresConfigServerAuth(displayName));
                instance.setPayload(payload);
                instance.setAvailable(payload != null);
            } catch (Exception exception) {
                instance.setErrorMessage(exception.getMessage());
            }
        } else {
            instance.setErrorMessage("Missing base URL");
        }

        GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> response = new GatewayServiceAggregateResponse<>();
        response.setServiceName(displayName);
        response.setInstances(List.of(instance));
        response.setTotalInstances(1);
        response.setHealthyInstances(Boolean.TRUE.equals(instance.getAvailable()) ? 1 : 0);
        response.setSummary(instance.getPayload());
        return response;
    }

    public GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> probeTcpModule(String displayName, String instanceId, String host, int port) {
        GatewayServiceInstanceResponse<GatewayServiceMetricsResponse> instance = new GatewayServiceInstanceResponse<>();
        instance.setInstanceId(instanceId);
        instance.setHost(host);
        instance.setIpAddress(host);
        instance.setPort(port);
        instance.setSecure(false);
        instance.setAvailable(false);

        GatewayServiceMetricsResponse payload = new GatewayServiceMetricsResponse();
        payload.setServiceName(displayName);
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), (int) instanceTimeoutMillis);
            instance.setAvailable(true);
            instance.setPayload(payload);
        } catch (Exception exception) {
            instance.setErrorMessage(exception.getMessage());
            instance.setPayload(payload);
        }

        GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> response = new GatewayServiceAggregateResponse<>();
        response.setServiceName(displayName);
        response.setInstances(List.of(instance));
        response.setTotalInstances(1);
        response.setHealthyInstances(Boolean.TRUE.equals(instance.getAvailable()) ? 1 : 0);
        response.setSummary(payload);
        return response;
    }

    public GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> configServerMetrics() {
        return aggregateSingletonMetrics("config-server", "config-server", configServerBaseUrl);
    }

    public GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> discoveryServerMetrics() {
        return aggregateSingletonMetrics("discovery-server", "discovery-server", trimEurekaPath(discoveryServerBaseUrl));
    }

    public GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> oauthServerMetrics() {
        return aggregateSingletonMetrics("oauth-server", "oauth-server", trimAuthPath(oauthServerBaseUrl));
    }

    public GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> redisMetrics() {
        return probeTcpModule("redis", "redis", redisHost, redisPort);
    }

    public GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> mysqlMetrics() {
        return probeTcpModule("mysql", "mysql", mysqlHost, mysqlPort);
    }

    private <T> List<GatewayServiceInstanceResponse<T>> fetchInstances(String requestedServiceId, String endpointPath, Class<T> responseType) {
        String resolvedServiceId = resolveServiceId(requestedServiceId);
        if (resolvedServiceId == null) {
            return List.of();
        }
        List<ServiceInstance> discoveredInstances = discoveryClient.getInstances(resolvedServiceId);
        List<GatewayServiceInstanceResponse<T>> instances = new ArrayList<>();
        for (ServiceInstance discoveredInstance : discoveredInstances) {
            instances.add(fetchInstance(discoveredInstance, endpointPath, responseType));
        }
        return instances;
    }

    private <T> GatewayServiceInstanceResponse<T> fetchInstance(ServiceInstance serviceInstance, String endpointPath, Class<T> responseType) {
        GatewayServiceInstanceResponse<T> instanceResponse = new GatewayServiceInstanceResponse<>();
        instanceResponse.setInstanceId(serviceInstance.getInstanceId());
        instanceResponse.setHost(serviceInstance.getHost());
        instanceResponse.setIpAddress(serviceInstance.getHost());
        instanceResponse.setPort(serviceInstance.getPort());
        instanceResponse.setSecure(serviceInstance.isSecure());
        instanceResponse.setAvailable(false);

        String baseUrl = serviceInstance.getUri().toString();
        String targetUrl = baseUrl + endpointPath;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofMillis(instanceTimeoutMillis))
                    .header("Accept", "application/json")
                    .header("X-Gateway-Token", gatewayInternalToken)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                instanceResponse.setErrorMessage("HTTP " + response.statusCode());
                return instanceResponse;
            }
            T payload = objectMapper.readValue(response.body(), responseType);
            instanceResponse.setPayload(payload);
            instanceResponse.setAvailable(payload != null);
        } catch (Exception exception) {
            instanceResponse.setErrorMessage(exception.getMessage());
        }
        return instanceResponse;
    }

    private String resolveServiceId(String requestedServiceId) {
        List<String> services = discoveryClient.getServices();
        for (String service : services) {
            if (service.equalsIgnoreCase(requestedServiceId)) {
                return service;
            }
        }
        return null;
    }

    private GatewayServiceMetricsResponse fetchSingletonMetrics(String displayName, String baseUrl, boolean useBasicAuth) throws Exception {
        try {
            return fetchActuatorMetrics(baseUrl, displayName, useBasicAuth);
        } catch (Exception exception) {
            GatewayServiceMetricsResponse fallback = fetchActuatorHealth(baseUrl, displayName, useBasicAuth);
            if (fallback == null) {
                throw exception;
            }
            return fallback;
        }
    }

    private GatewayServiceMetricsResponse fetchActuatorMetrics(String baseUrl, String displayName, boolean useBasicAuth) throws Exception {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(normalizedBaseUrl + "/actuator/prometheus"))
                .timeout(Duration.ofMillis(instanceTimeoutMillis))
                .header("Accept", MediaType.TEXT_PLAIN_VALUE);
        applyOptionalBasicAuth(builder, useBasicAuth);
        HttpRequest prometheusRequest = builder.GET().build();
        HttpResponse<String> prometheusResponse = httpClient.send(prometheusRequest, HttpResponse.BodyHandlers.ofString());
        if (prometheusResponse.statusCode() < 200 || prometheusResponse.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + prometheusResponse.statusCode());
        }
        String body = prometheusResponse.body();
        GatewayServiceMetricsResponse metrics = new GatewayServiceMetricsResponse();
        metrics.setServiceName(displayName);
        metrics.setUptimeSeconds(parsePrometheusGauge(body, "process_uptime_seconds"));
        metrics.setHeapUsedMb(toMegabytes(parsePrometheusGauge(body, "jvm_memory_used_bytes", "area=\"heap\"")));
        metrics.setHeapMaxMb(toMegabytes(parsePrometheusGauge(body, "jvm_memory_max_bytes", "area=\"heap\"")));
        metrics.setCpuUsagePercent(toPercent(parsePrometheusGauge(body, "system_cpu_usage")));
        metrics.setHttpRequestCount(parsePrometheusCounterSum(body, "http_server_requests_seconds_count"));
        metrics.setOperationCallCount(null);
        metrics.setAsyncQueueSize(null);
        metrics.setAsyncActiveCount(null);
        metrics.setOcrProcessedCount(null);
        return metrics;
    }

    private GatewayServiceMetricsResponse fetchActuatorHealth(String baseUrl, String displayName, boolean useBasicAuth) throws Exception {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(normalizedBaseUrl + "/actuator/health"))
                .timeout(Duration.ofMillis(instanceTimeoutMillis))
                .header("Accept", MediaType.APPLICATION_JSON_VALUE);
        applyOptionalBasicAuth(builder, useBasicAuth);
        HttpResponse<String> response = httpClient.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        if (!response.body().contains("\"status\":\"UP\"")) {
            throw new IllegalStateException("Health not UP");
        }
        GatewayServiceMetricsResponse metrics = new GatewayServiceMetricsResponse();
        metrics.setServiceName(displayName);
        return metrics;
    }

    private GatewayServiceMetricsResponse aggregateMetricsSummary(String displayName, List<GatewayServiceInstanceResponse<GatewayServiceMetricsResponse>> instances) {
        List<GatewayServiceMetricsResponse> availablePayloads = instances.stream()
                .filter(instance -> Boolean.TRUE.equals(instance.getAvailable()))
                .map(GatewayServiceInstanceResponse::getPayload)
                .filter(Objects::nonNull)
                .toList();
        if (availablePayloads.isEmpty()) {
            return null;
        }

        GatewayServiceMetricsResponse summary = new GatewayServiceMetricsResponse();
        summary.setServiceName(displayName);
        summary.setUptimeSeconds(availablePayloads.stream()
                .map(GatewayServiceMetricsResponse::getUptimeSeconds)
                .filter(Objects::nonNull)
                .min(Double::compareTo)
                .orElse(null));
        summary.setHeapUsedMb(sumMetric(availablePayloads.stream().map(GatewayServiceMetricsResponse::getHeapUsedMb).toList()));
        summary.setHeapMaxMb(sumMetric(availablePayloads.stream().map(GatewayServiceMetricsResponse::getHeapMaxMb).toList()));
        summary.setCpuUsagePercent(avgMetric(availablePayloads.stream().map(GatewayServiceMetricsResponse::getCpuUsagePercent).toList()));
        summary.setHttpRequestCount(sumMetric(availablePayloads.stream().map(GatewayServiceMetricsResponse::getHttpRequestCount).toList()));
        summary.setOperationCallCount(sumMetric(availablePayloads.stream().map(GatewayServiceMetricsResponse::getOperationCallCount).toList()));
        summary.setAsyncQueueSize(sumMetric(availablePayloads.stream().map(GatewayServiceMetricsResponse::getAsyncQueueSize).toList()));
        summary.setAsyncActiveCount(sumMetric(availablePayloads.stream().map(GatewayServiceMetricsResponse::getAsyncActiveCount).toList()));
        summary.setOcrProcessedCount(sumMetric(availablePayloads.stream().map(GatewayServiceMetricsResponse::getOcrProcessedCount).toList()));
        return summary;
    }

    private GatewayServiceHttpBreakdownResponse aggregateHttpSummary(String displayName, List<GatewayServiceInstanceResponse<GatewayServiceHttpBreakdownResponse>> instances) {
        List<GatewayServiceHttpBreakdownResponse> availablePayloads = instances.stream()
                .filter(instance -> Boolean.TRUE.equals(instance.getAvailable()))
                .map(GatewayServiceInstanceResponse::getPayload)
                .filter(Objects::nonNull)
                .toList();
        if (availablePayloads.isEmpty()) {
            return null;
        }

        Map<String, GatewayHttpEndpointMetricResponse> endpoints = new LinkedHashMap<>();
        double totalRequestCount = 0d;
        for (GatewayServiceHttpBreakdownResponse payload : availablePayloads) {
            totalRequestCount += payload.getTotalRequestCount();
            for (GatewayHttpEndpointMetricResponse endpoint : payload.getEndpoints()) {
                String method = endpoint.getMethod() == null ? "GET" : endpoint.getMethod();
                String key = method + " " + endpoint.getEndpoint();
                GatewayHttpEndpointMetricResponse aggregated = endpoints.computeIfAbsent(key, ignored -> {
                    GatewayHttpEndpointMetricResponse response = new GatewayHttpEndpointMetricResponse();
                    response.setMethod(method);
                    response.setEndpoint(endpoint.getEndpoint());
                    return response;
                });
                aggregated.setRequestCount(aggregated.getRequestCount() + endpoint.getRequestCount());
                aggregated.setMaxResponseTimeMs(Math.max(aggregated.getMaxResponseTimeMs(), endpoint.getMaxResponseTimeMs()));
            }
        }

        GatewayServiceHttpBreakdownResponse summary = new GatewayServiceHttpBreakdownResponse();
        summary.setServiceName(displayName);
        summary.setTotalRequestCount(totalRequestCount);
        summary.setEndpoints(endpoints.values().stream()
                .sorted(Comparator.comparingDouble(GatewayHttpEndpointMetricResponse::getRequestCount).reversed())
                .limit(12)
                .toList());
        return summary;
    }

    private GatewayServiceLogInsightsResponse aggregateLogSummary(String displayName, List<GatewayServiceInstanceResponse<GatewayServiceLogInsightsResponse>> instances) {
        List<GatewayServiceLogInsightsResponse> availablePayloads = instances.stream()
                .filter(instance -> Boolean.TRUE.equals(instance.getAvailable()))
                .map(GatewayServiceInstanceResponse::getPayload)
                .filter(Objects::nonNull)
                .toList();
        if (availablePayloads.isEmpty()) {
            return null;
        }

        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        Map<String, GatewayHourlyLogCountResponse> hourlyCounts = new LinkedHashMap<>();
        List<GatewayLogEventResponse> recentEvents = new ArrayList<>();
        long warnCountLastHour = 0L;
        long errorCountLastHour = 0L;

        for (GatewayServiceLogInsightsResponse payload : availablePayloads) {
            warnCountLastHour += defaultLong(payload.getWarnCountLastHour());
            errorCountLastHour += defaultLong(payload.getErrorCountLastHour());
            if (payload.getCategoryCounts() != null) {
                payload.getCategoryCounts().forEach((key, value) ->
                        categoryCounts.merge(key, value == null ? 0L : value, Long::sum));
            }
            if (payload.getHourlyCounts() != null) {
                for (GatewayHourlyLogCountResponse hourlyCount : payload.getHourlyCounts()) {
                    GatewayHourlyLogCountResponse aggregated = hourlyCounts.computeIfAbsent(hourlyCount.getHourLabel(), ignored -> {
                        GatewayHourlyLogCountResponse response = new GatewayHourlyLogCountResponse();
                        response.setHourLabel(hourlyCount.getHourLabel());
                        response.setWarnCount(0L);
                        response.setErrorCount(0L);
                        return response;
                    });
                    aggregated.setWarnCount(defaultLong(aggregated.getWarnCount()) + defaultLong(hourlyCount.getWarnCount()));
                    aggregated.setErrorCount(defaultLong(aggregated.getErrorCount()) + defaultLong(hourlyCount.getErrorCount()));
                }
            }
            if (payload.getRecentEvents() != null) {
                recentEvents.addAll(payload.getRecentEvents());
            }
        }

        GatewayServiceLogInsightsResponse summary = new GatewayServiceLogInsightsResponse();
        summary.setServiceName(displayName);
        summary.setWarnCountLastHour(warnCountLastHour);
        summary.setErrorCountLastHour(errorCountLastHour);
        summary.setCategoryCounts(categoryCounts);
        summary.setHourlyCounts(hourlyCounts.values().stream()
                .sorted(Comparator.comparing(GatewayHourlyLogCountResponse::getHourLabel))
                .toList());
        summary.setRecentEvents(recentEvents.stream()
                .sorted(Comparator.comparing(GatewayLogEventResponse::getTimestamp, Comparator.nullsLast(Long::compareTo)).reversed())
                .limit(20)
                .toList());
        return summary;
    }

    private Double sumMetric(List<Double> values) {
        double sum = 0d;
        boolean hasValue = false;
        for (Double value : values) {
            if (value == null) {
                continue;
            }
            sum += value;
            hasValue = true;
        }
        return hasValue ? sum : null;
    }

    private Double avgMetric(List<Double> values) {
        double sum = 0d;
        int count = 0;
        for (Double value : values) {
            if (value == null) {
                continue;
            }
            sum += value;
            count += 1;
        }
        return count > 0 ? sum / count : null;
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private Double parsePrometheusGauge(String body, String metricName, String... requiredLabels) {
        String[] lines = body.split("\\R");
        for (String line : lines) {
            if (line.startsWith("#") || !line.startsWith(metricName)) {
                continue;
            }
            boolean labelsMatch = true;
            for (String requiredLabel : requiredLabels) {
                if (!line.contains(requiredLabel)) {
                    labelsMatch = false;
                    break;
                }
            }
            if (!labelsMatch) {
                continue;
            }
            int lastSpace = line.lastIndexOf(' ');
            if (lastSpace < 0) {
                continue;
            }
            try {
                return Double.parseDouble(line.substring(lastSpace + 1).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Double parsePrometheusCounterSum(String body, String metricName) {
        double sum = 0d;
        boolean found = false;
        String[] lines = body.split("\\R");
        for (String line : lines) {
            if (line.startsWith("#") || !line.startsWith(metricName)) {
                continue;
            }
            int lastSpace = line.lastIndexOf(' ');
            if (lastSpace < 0) {
                continue;
            }
            try {
                sum += Double.parseDouble(line.substring(lastSpace + 1).trim());
                found = true;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return found ? sum : null;
    }

    private String trimEurekaPath(String baseUrl) {
        if (baseUrl == null) {
            return null;
        }
        return baseUrl.replaceAll("/eureka/?$", "");
    }

    private String trimAuthPath(String baseUrl) {
        if (baseUrl == null) {
            return null;
        }
        return baseUrl.replaceAll("/auth/?$", "");
    }

    private String extractHost(String baseUrl) {
        try {
            return URI.create(baseUrl).getHost();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer extractPort(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl);
            int port = uri.getPort();
            if (port > 0) {
                return port;
            }
            if ("https".equalsIgnoreCase(uri.getScheme())) {
                return 443;
            }
            if ("http".equalsIgnoreCase(uri.getScheme())) {
                return 80;
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Double toMegabytes(Double valueInBytes) {
        if (valueInBytes == null) {
            return null;
        }
        return valueInBytes / (1024d * 1024d);
    }

    private Double toPercent(Double ratio) {
        if (ratio == null) {
            return null;
        }
        return ratio * 100d;
    }

    private boolean requiresConfigServerAuth(String displayName) {
        return "config-server".equals(displayName);
    }

    private void applyOptionalBasicAuth(HttpRequest.Builder builder, boolean useBasicAuth) {
        if (!useBasicAuth) {
            return;
        }
        if (configServerUsername == null || configServerUsername.isBlank()) {
            return;
        }
        String password = configServerPassword == null ? "" : configServerPassword;
        String raw = configServerUsername + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        builder.header("Authorization", "Basic " + encoded);
    }
}
