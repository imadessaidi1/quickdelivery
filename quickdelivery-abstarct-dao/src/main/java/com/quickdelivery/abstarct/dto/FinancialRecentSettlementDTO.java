package com.quickdelivery.abstarct.dto;

import java.sql.Timestamp;

public class FinancialRecentSettlementDTO {
    private String packageReference;
    private String packageStatus;
    private Timestamp calculatedAt;
    private Double customerTotalPrice;
    private Double platformServiceFee;
    private Double platformCommissionAmount;
    private Double courierPayoutAmount;
    private String currency;

    public String getPackageReference() {
        return packageReference;
    }

    public void setPackageReference(String packageReference) {
        this.packageReference = packageReference;
    }

    public String getPackageStatus() {
        return packageStatus;
    }

    public void setPackageStatus(String packageStatus) {
        this.packageStatus = packageStatus;
    }

    public Timestamp getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Timestamp calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public Double getCustomerTotalPrice() {
        return customerTotalPrice;
    }

    public void setCustomerTotalPrice(Double customerTotalPrice) {
        this.customerTotalPrice = customerTotalPrice;
    }

    public Double getPlatformServiceFee() {
        return platformServiceFee;
    }

    public void setPlatformServiceFee(Double platformServiceFee) {
        this.platformServiceFee = platformServiceFee;
    }

    public Double getPlatformCommissionAmount() {
        return platformCommissionAmount;
    }

    public void setPlatformCommissionAmount(Double platformCommissionAmount) {
        this.platformCommissionAmount = platformCommissionAmount;
    }

    public Double getCourierPayoutAmount() {
        return courierPayoutAmount;
    }

    public void setCourierPayoutAmount(Double courierPayoutAmount) {
        this.courierPayoutAmount = courierPayoutAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
