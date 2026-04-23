package com.quickdelivery;

import com.quickdelivery.dto.GatewayHttpEndpointMetricResponse;
import com.quickdelivery.dto.GatewayServiceAggregateResponse;
import com.quickdelivery.dto.GatewayServiceHttpBreakdownResponse;
import com.quickdelivery.dto.GatewayServiceLogInsightsResponse;
import com.quickdelivery.dto.GatewayServiceMetricsResponse;
import com.quickdelivery.observability.RuntimeLogMonitor;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Statistic;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/gateway/v1/admin")
public class GatewayMetricsController {

    private final MeterRegistry meterRegistry;
    private final RuntimeLogMonitor runtimeLogMonitor;
    private final GatewayServiceAggregationService gatewayServiceAggregationService;

    public GatewayMetricsController(MeterRegistry meterRegistry,
                                    RuntimeLogMonitor runtimeLogMonitor,
                                    GatewayServiceAggregationService gatewayServiceAggregationService) {
        this.meterRegistry = meterRegistry;
        this.runtimeLogMonitor = runtimeLogMonitor;
        this.gatewayServiceAggregationService = gatewayServiceAggregationService;
    }

    @GetMapping("/metrics")
    @Cacheable(value = "gatewayAdminMetrics", key = "'singleton'", sync = true)
    public GatewayServiceMetricsResponse adminMetrics() {
        GatewayServiceMetricsResponse metrics = new GatewayServiceMetricsResponse();
        metrics.setServiceName("api-gateway");
        metrics.setUptimeSeconds(readGauge("process.uptime"));
        metrics.setHeapUsedMb(toMegabytes(readGauge("jvm.memory.used", "area", "heap")));
        metrics.setHeapMaxMb(toMegabytes(readGauge("jvm.memory.max", "area", "heap")));
        metrics.setCpuUsagePercent(toPercent(readGauge("system.cpu.usage")));
        metrics.setHttpRequestCount(sumMetric("http.server.requests"));
        metrics.setOperationCallCount(null);
        metrics.setAsyncQueueSize(null);
        metrics.setAsyncActiveCount(null);
        metrics.setOcrProcessedCount(null);
        return metrics;
    }

    @GetMapping("/log-insights")
    @Cacheable(value = "gatewayAdminLogInsights", key = "'singleton'", sync = true)
    public GatewayServiceLogInsightsResponse logInsights() {
        return runtimeLogMonitor.snapshot("api-gateway");
    }

    @GetMapping("/http-breakdown")
    @Cacheable(value = "gatewayAdminHttpBreakdown", key = "'singleton'", sync = true)
    public GatewayServiceHttpBreakdownResponse httpBreakdown() {
        GatewayServiceHttpBreakdownResponse response = new GatewayServiceHttpBreakdownResponse();
        response.setServiceName("api-gateway");

        Map<String, GatewayHttpEndpointMetricResponse> aggregated = new LinkedHashMap<>();
        meterRegistry.find("http.server.requests").meters().forEach(meter -> {
            String uri = meter.getId().getTag("uri");
            if (shouldIgnoreHttpUri(uri)) {
                return;
            }
            String method = meter.getId().getTag("method");
            double count = readStatistic(meter, Statistic.COUNT);
            if (count <= 0d) {
                return;
            }
            double maxResponseTimeMs = readStatistic(meter, Statistic.MAX) * 1000d;
            String key = (method == null ? "GET" : method) + " " + uri;
            GatewayHttpEndpointMetricResponse endpointMetric = aggregated.computeIfAbsent(key, ignored -> {
                GatewayHttpEndpointMetricResponse metric = new GatewayHttpEndpointMetricResponse();
                metric.setMethod(method == null ? "GET" : method);
                metric.setEndpoint(uri);
                return metric;
            });
            endpointMetric.setRequestCount(endpointMetric.getRequestCount() + count);
            endpointMetric.setMaxResponseTimeMs(Math.max(endpointMetric.getMaxResponseTimeMs(), maxResponseTimeMs));
        });

        List<GatewayHttpEndpointMetricResponse> endpoints = aggregated.values().stream()
                .sorted(Comparator.comparingDouble(GatewayHttpEndpointMetricResponse::getRequestCount).reversed())
                .limit(12)
                .toList();
        response.setEndpoints(endpoints);
        response.setTotalRequestCount(aggregated.values().stream().mapToDouble(GatewayHttpEndpointMetricResponse::getRequestCount).sum());
        return response;
    }

    @GetMapping("/users/metrics")
    @Cacheable(value = "gatewayUsersAdminMetrics", key = "'singleton'", sync = true)
    public GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> usersMetrics() {
        return gatewayServiceAggregationService.aggregateMetrics("users-service", "users-service", "/users/v1/admin/metrics");
    }

    @GetMapping("/users/http-breakdown")
    @Cacheable(value = "gatewayUsersAdminHttpBreakdown", key = "'singleton'", sync = true)
    public GatewayServiceAggregateResponse<GatewayServiceHttpBreakdownResponse> usersHttpBreakdown() {
        return gatewayServiceAggregationService.aggregateHttpBreakdown("users-service", "users-service", "/users/v1/admin/http-breakdown");
    }

    @GetMapping("/users/log-insights")
    @Cacheable(value = "gatewayUsersAdminLogInsights", key = "'singleton'", sync = true)
    public GatewayServiceAggregateResponse<GatewayServiceLogInsightsResponse> usersLogInsights() {
        return gatewayServiceAggregationService.aggregateLogInsights("users-service", "users-service", "/users/v1/admin/log-insights");
    }

    @GetMapping("/packages/metrics")
    public GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> packagesMetrics() {
        return gatewayServiceAggregationService.aggregateMetrics("package-service", "packages-service", "/packages/v1/admin/metrics");
    }

    @GetMapping("/tracking/metrics")
    @Cacheable(value = "gatewayTrackingAdminMetrics", key = "'singleton'", sync = true)
    public GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> trackingMetrics() {
        return gatewayServiceAggregationService.aggregateMetrics("package-service", "tracking-service", "/packages/v1/admin/tracking-metrics");
    }

    @GetMapping("/config/metrics")
    @Cacheable(value = "gatewayConfigAdminMetrics", key = "'singleton'", sync = true)
    public GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> configMetrics() {
        return gatewayServiceAggregationService.configServerMetrics();
    }

    @GetMapping("/discovery/metrics")
    @Cacheable(value = "gatewayDiscoveryAdminMetrics", key = "'singleton'", sync = true)
    public GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> discoveryMetrics() {
        return gatewayServiceAggregationService.discoveryServerMetrics();
    }

    @GetMapping("/oauth/metrics")
    @Cacheable(value = "gatewayOauthAdminMetrics", key = "'singleton'", sync = true)
    public GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> oauthMetrics() {
        return gatewayServiceAggregationService.oauthServerMetrics();
    }

    @GetMapping("/redis/metrics")
    @Cacheable(value = "gatewayRedisAdminMetrics", key = "'singleton'", sync = true)
    public GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> redisMetrics() {
        return gatewayServiceAggregationService.redisMetrics();
    }

    @GetMapping("/mysql/metrics")
    @Cacheable(value = "gatewayMysqlAdminMetrics", key = "'singleton'", sync = true)
    public GatewayServiceAggregateResponse<GatewayServiceMetricsResponse> mysqlMetrics() {
        return gatewayServiceAggregationService.mysqlMetrics();
    }

    @GetMapping("/packages/http-breakdown")
    @Cacheable(value = "gatewayPackagesAdminHttpBreakdown", key = "'singleton'", sync = true)
    public GatewayServiceAggregateResponse<GatewayServiceHttpBreakdownResponse> packagesHttpBreakdown() {
        return gatewayServiceAggregationService.aggregateHttpBreakdown("package-service", "packages-service", "/packages/v1/admin/http-breakdown");
    }

    @GetMapping("/packages/log-insights")
    @Cacheable(value = "gatewayPackagesAdminLogInsights", key = "'singleton'", sync = true)
    public GatewayServiceAggregateResponse<GatewayServiceLogInsightsResponse> packagesLogInsights() {
        return gatewayServiceAggregationService.aggregateLogInsights("package-service", "packages-service", "/packages/v1/admin/log-insights");
    }

    private Double readGauge(String meterName, String... tags) {
        try {
            Meter meter = meterRegistry.find(meterName).tags(tags).meter();
            if (meter == null) {
                return null;
            }
            return StreamSupport.stream(meter.measure().spliterator(), false)
                    .findFirst()
                    .map(Measurement::getValue)
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Double sumMetric(String meterName) {
        try {
            return meterRegistry.find(meterName).meters().stream()
                    .flatMap(meter -> StreamSupport.stream(meter.measure().spliterator(), false))
                    .mapToDouble(Measurement::getValue)
                    .sum();
        } catch (Exception ignored) {
            return null;
        }
    }

    private double readStatistic(Meter meter, Statistic statistic) {
        return StreamSupport.stream(meter.measure().spliterator(), false)
                .filter(measurement -> measurement.getStatistic() == statistic)
                .mapToDouble(Measurement::getValue)
                .findFirst()
                .orElse(0d);
    }

    private boolean shouldIgnoreHttpUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return true;
        }
        String normalizedUri = uri.toLowerCase(Locale.ROOT);
        return normalizedUri.startsWith("/actuator")
                || normalizedUri.startsWith("/gateway/v1/admin")
                || normalizedUri.contains("/admin/http-breakdown")
                || normalizedUri.contains("/admin/metrics")
                || normalizedUri.contains("/admin/log-insights")
                || "unknown".equals(normalizedUri)
                || "/error".equals(normalizedUri);
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
}
