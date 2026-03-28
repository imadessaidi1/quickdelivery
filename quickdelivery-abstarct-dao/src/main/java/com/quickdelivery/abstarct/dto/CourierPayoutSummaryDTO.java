package com.quickdelivery.abstarct.dto;

import java.sql.Timestamp;

public class CourierPayoutSummaryDTO {
    private Long deliveryPersonId;
    private String deliveryPersonName;
    private Long packageCount;
    private Double amount;
    private Timestamp oldestCreatedAt;
    private String currency;

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

    public Long getPackageCount() {
        return packageCount;
    }

    public void setPackageCount(Long packageCount) {
        this.packageCount = packageCount;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Timestamp getOldestCreatedAt() {
        return oldestCreatedAt;
    }

    public void setOldestCreatedAt(Timestamp oldestCreatedAt) {
        this.oldestCreatedAt = oldestCreatedAt;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
