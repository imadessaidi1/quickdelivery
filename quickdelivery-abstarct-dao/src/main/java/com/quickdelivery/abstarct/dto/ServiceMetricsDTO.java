package com.quickdelivery.abstarct.dto;

public class ServiceMetricsDTO {
    private String serviceName;
    private Double uptimeSeconds;
    private Double heapUsedMb;
    private Double heapMaxMb;
    private Double cpuUsagePercent;
    private Double httpRequestCount;
    private Double operationCallCount;
    private Double asyncQueueSize;
    private Double asyncActiveCount;
    private Double ocrProcessedCount;
    private Double googleMapsGeocodingCalls;
    private Double googleMapsDistanceMatrixCalls;
    private Double googleMapsDistanceCacheHits;
    private Double googleMapsClientMapLoads;
    private Double googleMapsClientPlacesCalls;
    private Double googleMapsClientRouteCalls;
    private Double googleMapsClientGeocodingCalls;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Double getUptimeSeconds() {
        return uptimeSeconds;
    }

    public void setUptimeSeconds(Double uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }

    public Double getHeapUsedMb() {
        return heapUsedMb;
    }

    public void setHeapUsedMb(Double heapUsedMb) {
        this.heapUsedMb = heapUsedMb;
    }

    public Double getHeapMaxMb() {
        return heapMaxMb;
    }

    public void setHeapMaxMb(Double heapMaxMb) {
        this.heapMaxMb = heapMaxMb;
    }

    public Double getCpuUsagePercent() {
        return cpuUsagePercent;
    }

    public void setCpuUsagePercent(Double cpuUsagePercent) {
        this.cpuUsagePercent = cpuUsagePercent;
    }

    public Double getHttpRequestCount() {
        return httpRequestCount;
    }

    public void setHttpRequestCount(Double httpRequestCount) {
        this.httpRequestCount = httpRequestCount;
    }

    public Double getOperationCallCount() {
        return operationCallCount;
    }

    public void setOperationCallCount(Double operationCallCount) {
        this.operationCallCount = operationCallCount;
    }

    public Double getAsyncQueueSize() {
        return asyncQueueSize;
    }

    public void setAsyncQueueSize(Double asyncQueueSize) {
        this.asyncQueueSize = asyncQueueSize;
    }

    public Double getAsyncActiveCount() {
        return asyncActiveCount;
    }

    public void setAsyncActiveCount(Double asyncActiveCount) {
        this.asyncActiveCount = asyncActiveCount;
    }

    public Double getOcrProcessedCount() {
        return ocrProcessedCount;
    }

    public void setOcrProcessedCount(Double ocrProcessedCount) {
        this.ocrProcessedCount = ocrProcessedCount;
    }

    public Double getGoogleMapsGeocodingCalls() {
        return googleMapsGeocodingCalls;
    }

    public void setGoogleMapsGeocodingCalls(Double googleMapsGeocodingCalls) {
        this.googleMapsGeocodingCalls = googleMapsGeocodingCalls;
    }

    public Double getGoogleMapsDistanceMatrixCalls() {
        return googleMapsDistanceMatrixCalls;
    }

    public void setGoogleMapsDistanceMatrixCalls(Double googleMapsDistanceMatrixCalls) {
        this.googleMapsDistanceMatrixCalls = googleMapsDistanceMatrixCalls;
    }

    public Double getGoogleMapsDistanceCacheHits() {
        return googleMapsDistanceCacheHits;
    }

    public void setGoogleMapsDistanceCacheHits(Double googleMapsDistanceCacheHits) {
        this.googleMapsDistanceCacheHits = googleMapsDistanceCacheHits;
    }

    public Double getGoogleMapsClientMapLoads() {
        return googleMapsClientMapLoads;
    }

    public void setGoogleMapsClientMapLoads(Double googleMapsClientMapLoads) {
        this.googleMapsClientMapLoads = googleMapsClientMapLoads;
    }

    public Double getGoogleMapsClientPlacesCalls() {
        return googleMapsClientPlacesCalls;
    }

    public void setGoogleMapsClientPlacesCalls(Double googleMapsClientPlacesCalls) {
        this.googleMapsClientPlacesCalls = googleMapsClientPlacesCalls;
    }

    public Double getGoogleMapsClientRouteCalls() {
        return googleMapsClientRouteCalls;
    }

    public void setGoogleMapsClientRouteCalls(Double googleMapsClientRouteCalls) {
        this.googleMapsClientRouteCalls = googleMapsClientRouteCalls;
    }

    public Double getGoogleMapsClientGeocodingCalls() {
        return googleMapsClientGeocodingCalls;
    }

    public void setGoogleMapsClientGeocodingCalls(Double googleMapsClientGeocodingCalls) {
        this.googleMapsClientGeocodingCalls = googleMapsClientGeocodingCalls;
    }
}
