package com.quickdelivery.abstarct.dto;

import java.util.ArrayList;
import java.util.List;

public class ServiceHttpBreakdownDTO {
    private String serviceName;
    private double totalRequestCount;
    private List<HttpEndpointMetricDTO> endpoints = new ArrayList<>();

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

    public List<HttpEndpointMetricDTO> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<HttpEndpointMetricDTO> endpoints) {
        this.endpoints = endpoints;
    }
}
