package com.quickdelivery.abstarct.entities;

import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
public class PackageSettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Integer version;

    @OneToOne
    @JoinColumn(name = "package_id", unique = true, nullable = false)
    private Package aPackage;

    @Column
    private Double customerTotalPrice;

    @Column
    private Double deliveryBaseAmount;

    @Column
    private Double insuranceFee;

    @Column
    private Double platformServiceFee;

    @Column
    private Double deliveryRevenueExcludingServiceFee;

    @Column
    private Double platformCommissionRate;

    @Column
    private Double platformCommissionAmount;

    @Column
    private Double courierShareRate;

    @Column
    private Double courierPayoutAmount;

    @Column
    private String currency;

    @Column
    private String pricingVersion;

    @Column
    private Timestamp calculatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Package getaPackage() {
        return aPackage;
    }

    public void setaPackage(Package aPackage) {
        this.aPackage = aPackage;
    }

    public Double getCustomerTotalPrice() {
        return customerTotalPrice;
    }

    public void setCustomerTotalPrice(Double customerTotalPrice) {
        this.customerTotalPrice = customerTotalPrice;
    }

    public Double getDeliveryBaseAmount() {
        return deliveryBaseAmount;
    }

    public void setDeliveryBaseAmount(Double deliveryBaseAmount) {
        this.deliveryBaseAmount = deliveryBaseAmount;
    }

    public Double getInsuranceFee() {
        return insuranceFee;
    }

    public void setInsuranceFee(Double insuranceFee) {
        this.insuranceFee = insuranceFee;
    }

    public Double getPlatformServiceFee() {
        return platformServiceFee;
    }

    public void setPlatformServiceFee(Double platformServiceFee) {
        this.platformServiceFee = platformServiceFee;
    }

    public Double getDeliveryRevenueExcludingServiceFee() {
        return deliveryRevenueExcludingServiceFee;
    }

    public void setDeliveryRevenueExcludingServiceFee(Double deliveryRevenueExcludingServiceFee) {
        this.deliveryRevenueExcludingServiceFee = deliveryRevenueExcludingServiceFee;
    }

    public Double getPlatformCommissionRate() {
        return platformCommissionRate;
    }

    public void setPlatformCommissionRate(Double platformCommissionRate) {
        this.platformCommissionRate = platformCommissionRate;
    }

    public Double getPlatformCommissionAmount() {
        return platformCommissionAmount;
    }

    public void setPlatformCommissionAmount(Double platformCommissionAmount) {
        this.platformCommissionAmount = platformCommissionAmount;
    }

    public Double getCourierShareRate() {
        return courierShareRate;
    }

    public void setCourierShareRate(Double courierShareRate) {
        this.courierShareRate = courierShareRate;
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

    public String getPricingVersion() {
        return pricingVersion;
    }

    public void setPricingVersion(String pricingVersion) {
        this.pricingVersion = pricingVersion;
    }

    public Timestamp getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Timestamp calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
