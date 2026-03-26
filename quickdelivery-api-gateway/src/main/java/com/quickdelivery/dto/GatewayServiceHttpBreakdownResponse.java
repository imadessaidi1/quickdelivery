package com.quickdelivery.dto;

import java.util.ArrayList;
import java.util.List;

public class GatewayServiceHttpBreakdownResponse {
    private String serviceName;
    private double totalRequestCount;
    private List<GatewayHttpEndpointMetricResponse> endpoints = new ArrayList<>();

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public double getTotalRequestCount() {
        return totalRequestCount;
    }

    public void setTotalRequestCount(double totalRequestCount) {
        this.totalRequestCount = totalRequestCount;
    }

    public List<GatewayHttpEndpointMetricResponse> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<GatewayHttpEndpointMetricResponse> endpoints) {
        this.endpoints = endpoints;
    }
}
