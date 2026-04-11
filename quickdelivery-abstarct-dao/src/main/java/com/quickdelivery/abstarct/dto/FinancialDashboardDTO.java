package com.quickdelivery.abstarct.dto;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class FinancialDashboardDTO {
    private Timestamp generatedAt;
    private String currency;
    private long settlementCount;
    private long deliveredPackageCount;
    private long pendingPayoutCount;
    private long paidPayoutCount;
    private Double customerRevenue;
    private Double platformServiceFees;
    private Double platformCommissionAmount;
    private Double platformMargin;
    private Double courierPayoutPendingAmount;
    private Double courierPayoutPaidAmount;
    private Long courierPenaltyCount;
    private Long courierActiveSuspensionCount;
    private Double courierFinancialPenaltyAmount;
    private Double averageOrderValue;
    private Double averageCourierPayout;
    private Double platformTakeRate;
    private List<FinancialTrendPointDTO> customerRevenueTrend = new ArrayList<>();
    private List<FinancialTrendPointDTO> platformMarginTrend = new ArrayList<>();
    private List<FinancialTrendPointDTO> courierPayoutTrend = new ArrayList<>();
    private List<FinancialRecentSettlementDTO> recentSettlements = new ArrayList<>();
    private List<CourierPayoutSummaryDTO> pendingPayouts = new ArrayList<>();
    private List<CourierPenaltyDTO> recentCourierPenalties = new ArrayList<>();

    public Timestamp getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Timestamp generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public long getSettlementCount() {
        return settlementCount;
    }

    public void setSettlementCount(long settlementCount) {
        this.settlementCount = settlementCount;
    }

    public long getDeliveredPackageCount() {
        return deliveredPackageCount;
    }

    public void setDeliveredPackageCount(long deliveredPackageCount) {
        this.deliveredPackageCount = deliveredPackageCount;
    }

    public long getPendingPayoutCount() {
        return pendingPayoutCount;
    }

    public void setPendingPayoutCount(long pendingPayoutCount) {
        this.pendingPayoutCount = pendingPayoutCount;
    }

    public long getPaidPayoutCount() {
        return paidPayoutCount;
    }

    public void setPaidPayoutCount(long paidPayoutCount) {
        this.paidPayoutCount = paidPayoutCount;
    }

    public Double getCustomerRevenue() {
        return customerRevenue;
    }

    public void setCustomerRevenue(Double customerRevenue) {
        this.customerRevenue = customerRevenue;
    }

    public Double getPlatformServiceFees() {
        return platformServiceFees;
    }

    public void setPlatformServiceFees(Double platformServiceFees) {
        this.platformServiceFees = platformServiceFees;
    }

    public Double getPlatformCommissionAmount() {
        return platformCommissionAmount;
    }

    public void setPlatformCommissionAmount(Double platformCommissionAmount) {
        this.platformCommissionAmount = platformCommissionAmount;
    }

    public Double getPlatformMargin() {
        return platformMargin;
    }

    public void setPlatformMargin(Double platformMargin) {
        this.platformMargin = platformMargin;
    }

    public Double getCourierPayoutPendingAmount() {
        return courierPayoutPendingAmount;
    }

    public void setCourierPayoutPendingAmount(Double courierPayoutPendingAmount) {
        this.courierPayoutPendingAmount = courierPayoutPendingAmount;
    }

    public Double getCourierPayoutPaidAmount() {
        return courierPayoutPaidAmount;
    }

    public void setCourierPayoutPaidAmount(Double courierPayoutPaidAmount) {
        this.courierPayoutPaidAmount = courierPayoutPaidAmount;
    }

    public Double getAverageOrderValue() {
        return averageOrderValue;
    }

    public Long getCourierPenaltyCount() {
        return courierPenaltyCount;
    }

    public void setCourierPenaltyCount(Long courierPenaltyCount) {
        this.courierPenaltyCount = courierPenaltyCount;
    }

    public Long getCourierActiveSuspensionCount() {
        return courierActiveSuspensionCount;
    }

    public void setCourierActiveSuspensionCount(Long courierActiveSuspensionCount) {
        this.courierActiveSuspensionCount = courierActiveSuspensionCount;
    }

    public Double getCourierFinancialPenaltyAmount() {
        return courierFinancialPenaltyAmount;
    }

    public void setCourierFinancialPenaltyAmount(Double courierFinancialPenaltyAmount) {
        this.courierFinancialPenaltyAmount = courierFinancialPenaltyAmount;
    }

    public void setAverageOrderValue(Double averageOrderValue) {
        this.averageOrderValue = averageOrderValue;
    }

    public Double getAverageCourierPayout() {
        return averageCourierPayout;
    }

    public void setAverageCourierPayout(Double averageCourierPayout) {
        this.averageCourierPayout = averageCourierPayout;
    }

    public Double getPlatformTakeRate() {
        return platformTakeRate;
    }

    public void setPlatformTakeRate(Double platformTakeRate) {
        this.platformTakeRate = platformTakeRate;
    }

    public List<FinancialTrendPointDTO> getCustomerRevenueTrend() {
        return customerRevenueTrend;
    }

    public void setCustomerRevenueTrend(List<FinancialTrendPointDTO> customerRevenueTrend) {
        this.customerRevenueTrend = customerRevenueTrend;
    }

    public List<FinancialTrendPointDTO> getPlatformMarginTrend() {
        return platformMarginTrend;
    }

    public void setPlatformMarginTrend(List<FinancialTrendPointDTO> platformMarginTrend) {
        this.platformMarginTrend = platformMarginTrend;
    }

    public List<FinancialTrendPointDTO> getCourierPayoutTrend() {
        return courierPayoutTrend;
    }

    public void setCourierPayoutTrend(List<FinancialTrendPointDTO> courierPayoutTrend) {
        this.courierPayoutTrend = courierPayoutTrend;
    }

    public List<FinancialRecentSettlementDTO> getRecentSettlements() {
        return recentSettlements;
    }

    public void setRecentSettlements(List<FinancialRecentSettlementDTO> recentSettlements) {
        this.recentSettlements = recentSettlements;
    }

    public List<CourierPayoutSummaryDTO> getPendingPayouts() {
        return pendingPayouts;
    }

    public void setPendingPayouts(List<CourierPayoutSummaryDTO> pendingPayouts) {
        this.pendingPayouts = pendingPayouts;
    }

    public List<CourierPenaltyDTO> getRecentCourierPenalties() {
        return recentCourierPenalties;
    }

    public void setRecentCourierPenalties(List<CourierPenaltyDTO> recentCourierPenalties) {
        this.recentCourierPenalties = recentCourierPenalties;
    }
}
