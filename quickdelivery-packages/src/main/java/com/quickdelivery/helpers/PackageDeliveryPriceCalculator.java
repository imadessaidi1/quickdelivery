package com.quickdelivery.helpers;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;
import com.quickdelivery.abstarct.dto.AddressDTO;
import com.quickdelivery.abstarct.dto.PackageDTO;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class PackageDeliveryPriceCalculator {
    private static final double LONG_DISTANCE_THRESHOLD_KM = 100.0;
    private static final double INSURANCE_MINIMUM_FEE = 1.00;
    private static final double INSURANCE_RATE = 0.02;
    private static final double PLATFORM_SERVICE_MINIMUM_FEE = 1.20;
    private static final double SHORT_DISTANCE_PLATFORM_SERVICE_RATE = 0.06;
    private static final double LONG_DISTANCE_PLATFORM_SERVICE_RATE = 0.06;
    private static final double PLATFORM_COMMISSION_RATE = 0.25;
    private static final double COURIER_PAYOUT_RATE = 0.75;
    private static final String DEFAULT_CURRENCY = "EUR";
    private static final String PRICING_VERSION = "v4";

    public static double calculateDeliveryPrice(GeoApiContext context, PackageDTO packageDTO) {
        double distance = calculateDistance(context, packageDTO.getAddresses());
        return calculateDeliveryPrice(distance, packageDTO);
    }

    public static double calculateDeliveryPrice(double distance, PackageDTO packageDTO) {
        return calculatePricingBreakdown(distance, packageDTO).customerTotalPrice();
    }

    public static PackagePricingBreakdown calculatePricingBreakdown(GeoApiContext context, PackageDTO packageDTO) {
        double distance = calculateDistance(context, packageDTO.getAddresses());
        return calculatePricingBreakdown(distance, packageDTO);
    }

    public static PackagePricingBreakdown calculatePricingBreakdown(double distance, PackageDTO packageDTO) {
        validateInputs(distance, packageDTO);
        boolean longDistance = Math.max(0, distance) > LONG_DISTANCE_THRESHOLD_KM;
        double basePrice = resolveCategoryBase(packageDTO.getPackageSizeCategory());
        double weightFees = calculateWeightSurcharge(packageDTO.getWeight());
        double distanceFees = longDistance
                ? calculateLongDistanceFees(distance)
                : calculateShortAndMediumDistanceFees(distance);
        double rawSubtotal = roundToCents(basePrice + weightFees + distanceFees);
        double modeCoefficient = resolveDeliveryModeCoefficient(packageDTO.getDeliverySpeed(), longDistance);
        double logisticPrice = roundToCents(rawSubtotal * modeCoefficient);
        double insuranceFees = roundToCents(calculateInsuranceFees(packageDTO.getInsuranceSelected(), packageDTO.getDeclaredValue()));
        double serviceFees = roundToCents(calculatePlatformServiceFees(logisticPrice + insuranceFees, longDistance));
        double rawFinalPrice = logisticPrice + insuranceFees + serviceFees;
        double flooredPrice = longDistance ? rawFinalPrice : Math.max(rawFinalPrice, resolveShortDistanceMinimumPrice(distance));
        double customerTotalPrice = roundUpToNearestTenth(flooredPrice);
        double deliveryRevenueExcludingServiceFee = roundToCents(Math.max(0, customerTotalPrice - serviceFees));
        double platformCommissionAmount = roundToCents(deliveryRevenueExcludingServiceFee * PLATFORM_COMMISSION_RATE);
        double courierPayoutAmount = roundToCents(deliveryRevenueExcludingServiceFee * COURIER_PAYOUT_RATE);
        return new PackagePricingBreakdown(
                customerTotalPrice,
                logisticPrice,
                insuranceFees,
                serviceFees,
                deliveryRevenueExcludingServiceFee,
                PLATFORM_COMMISSION_RATE,
                platformCommissionAmount,
                COURIER_PAYOUT_RATE,
                courierPayoutAmount,
                DEFAULT_CURRENCY,
                PRICING_VERSION
        );
    }

    private static double calculateDistance(GeoApiContext context, List<AddressDTO> addresses) {
        try {
            DirectionsResult result = DirectionsApi.newRequest(context)
                    .origin(addresses.get(0).toString())
                    .destination(addresses.get(1).toString())
                    .mode(TravelMode.DRIVING)
                    .await();
            long distanceInMeters = result.routes[0].legs[0].distance.inMeters;
            return distanceInMeters / 1000.0;
        } catch (ApiException | InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static double resolveCategoryBase(String packageSizeCategory) {
        String normalizedCategory = normalizeCategory(packageSizeCategory);
        return switch (normalizedCategory) {
            case "SMALL" -> 4.50;
            case "MEDIUM" -> 6.50;
            case "LARGE" -> 8.50;
            case "EXTRA_LARGE" -> 11.00;
            default -> throw new IllegalArgumentException("Unknown package category: " + packageSizeCategory);
        };
    }

    private static String normalizeCategory(String packageSizeCategory) {
        if (packageSizeCategory == null || packageSizeCategory.isBlank()) {
            throw new IllegalArgumentException("Package category is required.");
        }
        String normalized = packageSizeCategory.trim().toUpperCase(Locale.ROOT);
        if ("XLARGE".equals(normalized) || "X_LARGE".equals(normalized) || "X-LARGE".equals(normalized)) {
            return "EXTRA_LARGE";
        }
        if ("EXTRALARGE".equals(normalized) || "EXTRA-LARGE".equals(normalized)) {
            return "EXTRA_LARGE";
        }
        return normalized;
    }

    private static double calculateWeightSurcharge(Float weight) {
        double realWeight = Math.max(0, weight == null ? 0 : weight);
        if (realWeight <= 0) {
            throw new IllegalArgumentException("Weight must be greater than zero.");
        }
        if (realWeight <= 2) {
            return 0;
        }
        if (realWeight <= 5) {
            return 1.50;
        }
        if (realWeight <= 10) {
            return 3.00;
        }
        if (realWeight <= 15) {
            return 5.00;
        }
        return 8.00;
    }

    private static double calculateShortAndMediumDistanceFees(double distance) {
        double normalizedDistance = Math.max(0, distance);
        double firstTier = Math.min(normalizedDistance, 10.0);
        double secondTier = Math.min(Math.max(normalizedDistance - 10.0, 0), 20.0);
        double thirdTier = Math.min(Math.max(normalizedDistance - 30.0, 0), 20.0);
        double fourthTier = Math.min(Math.max(normalizedDistance - 50.0, 0), 50.0);
        return roundToCents((firstTier * 0.45) + (secondTier * 0.28) + (thirdTier * 0.18) + (fourthTier * 0.12));
    }

    private static double calculateLongDistanceFees(double distance) {
        double normalizedDistance = Math.max(0, distance);
        double firstTier = Math.min(normalizedDistance, 20.0);
        double secondTier = Math.min(Math.max(normalizedDistance - 20.0, 0), 80.0);
        double thirdTier = Math.max(normalizedDistance - 100.0, 0);
        return roundToCents((firstTier * 0.35) + (secondTier * 0.08) + (thirdTier * 0.02));
    }

    private static double resolveDeliveryModeCoefficient(String deliverySpeed, boolean longDistance) {
        if (deliverySpeed == null || deliverySpeed.isBlank()) {
            throw new IllegalArgumentException("Delivery mode is required.");
        }
        String speed = deliverySpeed.trim().toUpperCase(Locale.ROOT);
        if (longDistance) {
            return switch (speed) {
                case "STANDARD" -> 0.80;
                case "EXPRESS" -> 1.00;
                case "SAME_DAY", "SAMEDAY" -> 1.20;
                default -> throw new IllegalArgumentException("Unknown delivery mode: " + deliverySpeed);
            };
        }
        return switch (speed) {
            case "STANDARD" -> 1.00;
            case "EXPRESS" -> 1.15;
            case "SAME_DAY", "SAMEDAY" -> 1.30;
            default -> throw new IllegalArgumentException("Unknown delivery mode: " + deliverySpeed);
        };
    }

    private static double calculateInsuranceFees(Boolean insuranceSelected, Double declaredValue) {
        if (!Boolean.TRUE.equals(insuranceSelected)) {
            return 0;
        }
        if (declaredValue == null || declaredValue <= 0) {
            throw new IllegalArgumentException("Declared value must be greater than zero when insurance is selected.");
        }
        double insuredValue = declaredValue;
        return Math.max(INSURANCE_MINIMUM_FEE, insuredValue * INSURANCE_RATE);
    }

    private static double calculatePlatformServiceFees(double deliveryPriceWithInsurance, boolean longDistance) {
        double rate = longDistance ? LONG_DISTANCE_PLATFORM_SERVICE_RATE : SHORT_DISTANCE_PLATFORM_SERVICE_RATE;
        return Math.max(PLATFORM_SERVICE_MINIMUM_FEE, deliveryPriceWithInsurance * rate);
    }

    private static double resolveShortDistanceMinimumPrice(double distance) {
        if (distance <= 10.0) {
            return 9.90;
        }
        if (distance <= 30.0) {
            return 12.90;
        }
        if (distance <= 50.0) {
            return 15.90;
        }
        return 19.90;
    }

    private static void validateInputs(double distance, PackageDTO packageDTO) {
        if (distance <= 0) {
            throw new IllegalArgumentException("Distance must be greater than zero.");
        }
        if (packageDTO == null) {
            throw new IllegalArgumentException("Package payload is required.");
        }
        normalizeCategory(packageDTO.getPackageSizeCategory());
        calculateWeightSurcharge(packageDTO.getWeight());
        resolveDeliveryModeCoefficient(packageDTO.getDeliverySpeed(), distance > LONG_DISTANCE_THRESHOLD_KM);
        if (Boolean.TRUE.equals(packageDTO.getInsuranceSelected())
                && (packageDTO.getDeclaredValue() == null || packageDTO.getDeclaredValue() <= 0)) {
            throw new IllegalArgumentException("Declared value must be greater than zero when insurance is selected.");
        }
    }

    private static double roundUpToNearestTenth(double value) {
        return Math.ceil(value * 10.0) / 10.0;
    }

    private static double roundToCents(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
