package com.quickdelivery.dto;

public class RoutePlanMetricsDTO {
    private Double totalDistanceMeters;
    private Double directDistanceMeters;
    private Double detourMeters;
    private Double detourRatio;
    private Double estimatedDurationMinutes;
    private Double totalDisplayedAmount;
    private Double totalWeightKg;
    private Double totalVolumeCm3;
    private Double payoutPerKm;
    private Double payoutPerHour;
    private Double qualityScore;

    public Double getTotalDistanceMeters() {
        return totalDistanceMeters;
    }

    public void setTotalDistanceMeters(Double totalDistanceMeters) {
        this.totalDistanceMeters = totalDistanceMeters;
    }

    public Double getDirectDistanceMeters() {
        return directDistanceMeters;
    }

    public void setDirectDistanceMeters(Double directDistanceMeters) {
        this.directDistanceMeters = directDistanceMeters;
    }

    public Double getDetourMeters() {
        return detourMeters;
    }

    public void setDetourMeters(Double detourMeters) {
        this.detourMeters = detourMeters;
    }

    public Double getDetourRatio() {
        return detourRatio;
    }

    public void setDetourRatio(Double detourRatio) {
        this.detourRatio = detourRatio;
    }

    public Double getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Double estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public Double getTotalDisplayedAmount() {
        return totalDisplayedAmount;
    }

    public void setTotalDisplayedAmount(Double totalDisplayedAmount) {
        this.totalDisplayedAmount = totalDisplayedAmount;
    }

    public Double getTotalWeightKg() {
        return totalWeightKg;
    }

    public void setTotalWeightKg(Double totalWeightKg) {
        this.totalWeightKg = totalWeightKg;
    }

    public Double getTotalVolumeCm3() {
        return totalVolumeCm3;
    }

    public void setTotalVolumeCm3(Double totalVolumeCm3) {
        this.totalVolumeCm3 = totalVolumeCm3;
    }

    public Double getPayoutPerKm() {
        return payoutPerKm;
    }

    public void setPayoutPerKm(Double payoutPerKm) {
        this.payoutPerKm = payoutPerKm;
    }

    public Double getPayoutPerHour() {
        return payoutPerHour;
    }

    public void setPayoutPerHour(Double payoutPerHour) {
        this.payoutPerHour = payoutPerHour;
    }

    public Double getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Double qualityScore) {
        this.qualityScore = qualityScore;
    }
}
