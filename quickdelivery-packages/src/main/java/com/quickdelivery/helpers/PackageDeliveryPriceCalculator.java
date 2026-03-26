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

public class PackageDeliveryPriceCalculator {
    private static final double MINIMUM_PRICE = 6.90;
    private static final double STANDARD_BASE_PRICE = 4.90;
    private static final double EXPRESS_BASE_PRICE = 7.90;
    private static final double SAME_DAY_BASE_PRICE = 10.90;
    private static final double STANDARD_DISTANCE_FACTOR = 1.00;
    private static final double EXPRESS_DISTANCE_FACTOR = 1.15;
    private static final double SAME_DAY_DISTANCE_FACTOR = 1.30;
    private static final double INSURANCE_MINIMUM_FEE = 1.50;
    private static final double INSURANCE_RATE = 0.02;

    public static double calculateDeliveryPrice(GeoApiContext context, PackageDTO packageDTO) {
        double distance = calculateDistance(context, packageDTO.getAddresses());
        return calculateDeliveryPrice(distance, packageDTO);
    }

    public static double calculateDeliveryPrice(double distance, PackageDTO packageDTO) {
        double chargeableWeight = calculateChargeableWeight(packageDTO.getDepth(), packageDTO.getWidth(), packageDTO.getHeight(), packageDTO.getWeight());
        double basePrice = resolveBasePrice(packageDTO.getDeliverySpeed());
        double distanceFees = calculateDistanceFees(distance, packageDTO.getDeliverySpeed());
        double weightFees = calculateWeightFees(chargeableWeight);
        double floorFees = calculateFloorFees(
                chargeableWeight,
                packageDTO.getAddresses().get(0).getFloor(),
                packageDTO.getAddresses().get(0).getHasElevator(),
                packageDTO.getAddresses().get(1).getFloor(),
                packageDTO.getAddresses().get(1).getHasElevator()
        );
        double insuranceFees = calculateInsuranceFees(packageDTO.getInsuranceSelected(), packageDTO.getDeclaredValue());
        double totalPrice = basePrice + distanceFees + weightFees + floorFees + insuranceFees;
        return roundUpToNearestTenth(Math.max(MINIMUM_PRICE, totalPrice));
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

    private static double calculateChargeableWeight(Float length, Float width, Float height, Float actualWeight) {
        double realWeight = Math.max(0.5, actualWeight == null ? 0 : actualWeight);
        double volumetricWeight = calculateVolumetricWeight(length, width, height);
        return Math.max(realWeight, volumetricWeight);
    }

    private static double calculateVolumetricWeight(Float length, Float width, Float height) {
        double resolvedLength = Math.max(0, length == null ? 0 : length);
        double resolvedWidth = Math.max(0, width == null ? 0 : width);
        double resolvedHeight = Math.max(0, height == null ? 0 : height);
        if (resolvedLength == 0 || resolvedWidth == 0 || resolvedHeight == 0) {
            return 0;
        }
        return (resolvedLength * resolvedWidth * resolvedHeight) / 5000.0;
    }

    private static double calculateDistanceFees(double distance, String deliverySpeed) {
        double rawDistanceFees;
        if (distance <= 3) {
            rawDistanceFees = 1.20 * distance;
        } else if (distance <= 10) {
            rawDistanceFees = 3.60 + (0.95 * (distance - 3));
        } else {
            rawDistanceFees = 10.25 + (0.80 * (distance - 10));
        }
        return rawDistanceFees * resolveDistanceFactor(deliverySpeed);
    }

    private static double calculateWeightFees(double chargeableWeight) {
        if (chargeableWeight <= 2) {
            return 0;
        }
        if (chargeableWeight <= 10) {
            return 0.60 * (chargeableWeight - 2);
        }
        return 4.80 + (0.90 * (chargeableWeight - 10));
    }

    private static double calculateFloorFees(double chargeableWeight,
                                             Integer departureFloor,
                                             Boolean departureHasElevator,
                                             Integer arrivalFloor,
                                             Boolean arrivalHasElevator) {
        double departureFees = calculateOneAddressFloorFees(chargeableWeight, departureFloor, departureHasElevator);
        double arrivalFees = calculateOneAddressFloorFees(chargeableWeight, arrivalFloor, arrivalHasElevator);
        return Math.min(6.00, departureFees + arrivalFees);
    }

    private static double calculateOneAddressFloorFees(double chargeableWeight, Integer floor, Boolean hasElevator) {
        int normalizedFloor = Math.max(0, floor == null ? 0 : floor);
        if (normalizedFloor == 0) {
            return 0;
        }
        boolean elevator = Boolean.TRUE.equals(hasElevator);
        double noElevatorFee = chargeableWeight <= 5 ? 0.80 : 1.20;
        double elevatorFee = chargeableWeight <= 5 ? 0.35 : 0.55;
        return normalizedFloor * (elevator ? elevatorFee : noElevatorFee);
    }

    private static double calculateInsuranceFees(Boolean insuranceSelected, Double declaredValue) {
        if (!Boolean.TRUE.equals(insuranceSelected)) {
            return 0;
        }
        double insuredValue = Math.max(0, declaredValue == null ? 0 : declaredValue);
        return Math.max(INSURANCE_MINIMUM_FEE, insuredValue * INSURANCE_RATE);
    }

    private static double resolveBasePrice(String deliverySpeed) {
        String speed = deliverySpeed == null ? "STANDARD" : deliverySpeed.trim().toUpperCase();
        return switch (speed) {
            case "EXPRESS" -> EXPRESS_BASE_PRICE;
            case "SAMEDAY" -> SAME_DAY_BASE_PRICE;
            default -> STANDARD_BASE_PRICE;
        };
    }

    private static double resolveDistanceFactor(String deliverySpeed) {
        String speed = deliverySpeed == null ? "STANDARD" : deliverySpeed.trim().toUpperCase();
        return switch (speed) {
            case "EXPRESS" -> EXPRESS_DISTANCE_FACTOR;
            case "SAMEDAY" -> SAME_DAY_DISTANCE_FACTOR;
            default -> STANDARD_DISTANCE_FACTOR;
        };
    }

    private static double roundUpToNearestTenth(double value) {
        return Math.ceil(value * 10.0) / 10.0;
    }
}
