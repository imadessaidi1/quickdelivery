package com.quickdelivery.abstarct.dto;

import java.sql.Timestamp;

public class CourierPenaltyDTO {
    private Long id;
    private Long deliveryPersonId;
    private String deliveryPersonName;
    private String deliveryPersonEmail;
    private Long deliveryRouteId;
    private Long packageId;
    private String packageReference;
    private String type;
    private String status;
    private Integer severity;
    private String reason;
    private String detailsJson;
    private Double financialPenaltyAmount;
    private String currency;
    private Timestamp suspensionUntil;
    private Timestamp createdAt;
    private Timestamp reviewedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDeliveryPersonId() {
        return deliveryPersonId;
    }

    public void setDeliveryPersonId(Long deliveryPersonId) {
        this.deliveryPersonId = deliveryPersonId;
    }

    public String getDeliveryPersonName() {
        return deliveryPersonName;
    }

    public void setDeliveryPersonName(String deliveryPersonName) {
        this.deliveryPersonName = deliveryPersonName;
    }

    public String getDeliveryPersonEmail() {
        return deliveryPersonEmail;
    }

    public void setDeliveryPersonEmail(String deliveryPersonEmail) {
        this.deliveryPersonEmail = deliveryPersonEmail;
    }

    public Long getDeliveryRouteId() {
        return deliveryRouteId;
    }

    public void setDeliveryRouteId(Long deliveryRouteId) {
        this.deliveryRouteId = deliveryRouteId;
    }

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public String getPackageReference() {
        return packageReference;
    }

    public void setPackageReference(String packageReference) {
        this.packageReference = packageReference;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSeverity() {
        return severity;
    }

    public void setSeverity(Integer severity) {
        this.severity = severity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }

    public Double getFinancialPenaltyAmount() {
        return financialPenaltyAmount;
    }

    public void setFinancialPenaltyAmount(Double financialPenaltyAmount) {
        this.financialPenaltyAmount = financialPenaltyAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Timestamp getSuspensionUntil() {
        return suspensionUntil;
    }

    public void setSuspensionUntil(Timestamp suspensionUntil) {
        this.suspensionUntil = suspensionUntil;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
