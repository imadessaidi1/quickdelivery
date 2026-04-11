package com.quickdelivery.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RoutePlanDTO {
    private Long routeId;
    private String mode;
    private String status;
    private Long selectedPackageId;
    private RoutePlanPointDTO start;
    private RoutePlanPointDTO end;
    private List<Long> packageIds = new ArrayList<>();
    private List<RoutePlanStopDTO> stops = new ArrayList<>();
    private Map<Long, RoutePlanPackageAnnotationDTO> packageAnnotations = new LinkedHashMap<>();
    private RoutePlanMetricsDTO metrics;
    private String googleMapsNavigationUrl;
    private List<String> googleMapsNavigationUrls = new ArrayList<>();

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getSelectedPackageId() {
        return selectedPackageId;
    }

    public void setSelectedPackageId(Long selectedPackageId) {
        this.selectedPackageId = selectedPackageId;
    }

    public RoutePlanPointDTO getStart() {
        return start;
    }

    public void setStart(RoutePlanPointDTO start) {
        this.start = start;
    }

    public RoutePlanPointDTO getEnd() {
        return end;
    }

    public void setEnd(RoutePlanPointDTO end) {
        this.end = end;
    }

    public List<Long> getPackageIds() {
        return packageIds;
    }

    public void setPackageIds(List<Long> packageIds) {
        this.packageIds = packageIds;
    }

    public List<RoutePlanStopDTO> getStops() {
        return stops;
    }

    public void setStops(List<RoutePlanStopDTO> stops) {
        this.stops = stops;
    }

    public Map<Long, RoutePlanPackageAnnotationDTO> getPackageAnnotations() {
        return packageAnnotations;
    }

    public void setPackageAnnotations(Map<Long, RoutePlanPackageAnnotationDTO> packageAnnotations) {
        this.packageAnnotations = packageAnnotations;
    }

    public RoutePlanMetricsDTO getMetrics() {
        return metrics;
    }

    public void setMetrics(RoutePlanMetricsDTO metrics) {
        this.metrics = metrics;
    }

    public String getGoogleMapsNavigationUrl() {
        return googleMapsNavigationUrl;
    }

    public void setGoogleMapsNavigationUrl(String googleMapsNavigationUrl) {
        this.googleMapsNavigationUrl = googleMapsNavigationUrl;
    }

    public List<String> getGoogleMapsNavigationUrls() {
        return googleMapsNavigationUrls;
    }

    public void setGoogleMapsNavigationUrls(List<String> googleMapsNavigationUrls) {
        this.googleMapsNavigationUrls = googleMapsNavigationUrls;
    }
}
