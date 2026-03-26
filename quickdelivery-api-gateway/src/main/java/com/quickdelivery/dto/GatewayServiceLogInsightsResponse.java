package com.quickdelivery.dto;

import java.util.List;
import java.util.Map;

public class GatewayServiceLogInsightsResponse {
    private String serviceName;
    private Long warnCountLastHour;
    private Long errorCountLastHour;
    private Map<String, Long> categoryCounts;
    private List<GatewayHourlyLogCountResponse> hourlyCounts;
    private List<GatewayLogEventResponse> recentEvents;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Long getWarnCountLastHour() {
        return warnCountLastHour;
    }

    public void setWarnCountLastHour(Long warnCountLastHour) {
        this.warnCountLastHour = warnCountLastHour;
    }

    public Long getErrorCountLastHour() {
        return errorCountLastHour;
    }

    public void setErrorCountLastHour(Long errorCountLastHour) {
        this.errorCountLastHour = errorCountLastHour;
    }

    public Map<String, Long> getCategoryCounts() {
        return categoryCounts;
    }

    public void setCategoryCounts(Map<String, Long> categoryCounts) {
        this.categoryCounts = categoryCounts;
    }

    public List<GatewayHourlyLogCountResponse> getHourlyCounts() {
        return hourlyCounts;
    }

    public void setHourlyCounts(List<GatewayHourlyLogCountResponse> hourlyCounts) {
        this.hourlyCounts = hourlyCounts;
    }

    public List<GatewayLogEventResponse> getRecentEvents() {
        return recentEvents;
    }

    public void setRecentEvents(List<GatewayLogEventResponse> recentEvents) {
        this.recentEvents = recentEvents;
    }
}
