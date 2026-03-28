package com.quickdelivery.helpers;

public record PackagePricingBreakdown(
        double customerTotalPrice,
        double deliveryBaseAmount,
        double insuranceFee,
        double platformServiceFee,
        double deliveryRevenueExcludingServiceFee,
        double platformCommissionRate,
        double platformCommissionAmount,
        double courierShareRate,
        double courierPayoutAmount,
        String currency,
        String pricingVersion
) {
}
