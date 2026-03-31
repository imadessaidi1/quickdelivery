package com.quickdelivery.abstarct.dto;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminPackageDashboardSummaryDTO {
    private Timestamp generatedAt;
    private int year;
    private Map<String, Long> statusCounts = new LinkedHashMap<>();
    private List<Long> monthlyShipmentCounts = new ArrayList<>();
    private List<Double> monthlyDeliveredRevenue = new ArrayList<>();
    private List<PackageDTO> recentPackages = new ArrayList<>();

    public Timestamp getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Timestamp generatedAt) {
        this.generatedAt = generatedAt;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public Map<String, Long> getStatusCounts() {
        return statusCounts;
    }

    public void setStatusCounts(Map<String, Long> statusCounts) {
        this.statusCounts = statusCounts;
    }

    public List<Long> getMonthlyShipmentCounts() {
        return monthlyShipmentCounts;
    }

    public void setMonthlyShipmentCounts(List<Long> monthlyShipmentCounts) {
        this.monthlyShipmentCounts = monthlyShipmentCounts;
    }

    public List<Double> getMonthlyDeliveredRevenue() {
        return monthlyDeliveredRevenue;
    }

    public void setMonthlyDeliveredRevenue(List<Double> monthlyDeliveredRevenue) {
        this.monthlyDeliveredRevenue = monthlyDeliveredRevenue;
    }

    public List<PackageDTO> getRecentPackages() {
        return recentPackages;
    }

    public void setRecentPackages(List<PackageDTO> recentPackages) {
        this.recentPackages = recentPackages;
    }
}
