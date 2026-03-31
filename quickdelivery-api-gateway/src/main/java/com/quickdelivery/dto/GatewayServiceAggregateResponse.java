package com.quickdelivery.dto;

import java.util.ArrayList;
import java.util.List;

public class GatewayServiceAggregateResponse<T> {
    private String serviceName;
    private int totalInstances;
    private int healthyInstances;
    private T summary;
    private List<GatewayServiceInstanceResponse<T>> instances = new ArrayList<>();

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public int getTotalInstances() {
        return totalInstances;
    }

    public void setTotalInstances(int totalInstances) {
        this.totalInstances = totalInstances;
    }

    public int getHealthyInstances() {
        return healthyInstances;
    }

    public void setHealthyInstances(int healthyInstances) {
        this.healthyInstances = healthyInstances;
    }

    public T getSummary() {
        return summary;
    }

    public void setSummary(T summary) {
        this.summary = summary;
    }

    public List<GatewayServiceInstanceResponse<T>> getInstances() {
        return instances;
    }

    public void setInstances(List<GatewayServiceInstanceResponse<T>> instances) {
        this.instances = instances;
    }
}
