package com.quickdelivery.dto;

public class GatewayHttpEndpointMetricResponse {
    private String endpoint;
    private String method;
    private double requestCount;
    private double maxResponseTimeMs;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public double getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(double requestCount) {
        this.requestCount = requestCount;
    }

    public double getMaxResponseTimeMs() {
        return maxResponseTimeMs;
    }

    public void setMaxResponseTimeMs(double maxResponseTimeMs) {
        this.maxResponseTimeMs = maxResponseTimeMs;
    }
}
