package com.quickdelivery.helpers;

import com.quickdelivery.abstarct.dto.AddressDTO;
import com.quickdelivery.abstarct.dto.PackageDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PackageDeliveryPriceCalculatorTest {

    @Test
    void mediumStandardWithInsuranceUsesSpecExample() {
        PackageDTO packageDTO = buildPackage("MEDIUM", 40, 30, 30, 10, "STANDARD", true, 50);

        PackagePricingBreakdown breakdown = PackageDeliveryPriceCalculator.calculatePricingBreakdown(35.0, packageDTO);

        assertEquals(22.8, breakdown.customerTotalPrice(), 0.0001);
        assertEquals(20.5, breakdown.deliveryBaseAmount(), 0.0001);
        assertEquals(1.0, breakdown.insuranceFee(), 0.0001);
        assertEquals(1.29, breakdown.platformServiceFee(), 0.0001);
        assertEquals("v4", breakdown.pricingVersion());
    }

    @Test
    void standardExpressAndSameDayRespectModeCoefficients() {
        PackageDTO standard = buildPackage("SMALL", 20, 20, 20, 2, "STANDARD", false, 0);
        PackageDTO express = buildPackage("SMALL", 20, 20, 20, 2, "EXPRESS", false, 0);
        PackageDTO sameDay = buildPackage("SMALL", 20, 20, 20, 2, "SAME_DAY", false, 0);

        assertEquals(10.2, PackageDeliveryPriceCalculator.calculateDeliveryPrice(10, standard), 0.0001);
        assertEquals(11.6, PackageDeliveryPriceCalculator.calculateDeliveryPrice(10, express), 0.0001);
        assertEquals(12.9, PackageDeliveryPriceCalculator.calculateDeliveryPrice(10, sameDay), 0.0001);
    }

    @Test
    void weightSurchargeUsesRealWeightOnly() {
        PackageDTO packageDTO = buildPackage("SMALL", 120, 120, 120, 3, "STANDARD", false, 0);

        PackagePricingBreakdown breakdown = PackageDeliveryPriceCalculator.calculatePricingBreakdown(5.0, packageDTO);

        assertEquals(9.9, breakdown.customerTotalPrice(), 0.0001);
        assertEquals(8.25, breakdown.deliveryBaseAmount(), 0.0001);
    }

    @Test
    void insuranceUsesMinimumOneEuroOrTwoPercent() {
        PackageDTO lowValuePackage = buildPackage("MEDIUM", 40, 30, 30, 3, "STANDARD", true, 50);
        PackageDTO highValuePackage = buildPackage("MEDIUM", 40, 30, 30, 3, "STANDARD", true, 300);

        assertEquals(12.9, PackageDeliveryPriceCalculator.calculateDeliveryPrice(6, lowValuePackage), 0.0001);
        assertEquals(17.9, PackageDeliveryPriceCalculator.calculateDeliveryPrice(6, highValuePackage), 0.0001);
    }

    @Test
    void distancePricingIsDegressive() {
        PackageDTO packageDTO = buildPackage("LARGE", 60, 40, 40, 8, "SAME_DAY", false, 0);

        PackagePricingBreakdown breakdown = PackageDeliveryPriceCalculator.calculatePricingBreakdown(35.0, packageDTO);

        assertEquals(31.1, breakdown.customerTotalPrice(), 0.0001);
        assertEquals(29.25, breakdown.deliveryBaseAmount(), 0.0001);
        assertEquals(0.0, breakdown.insuranceFee(), 0.0001);
        assertEquals(1.76, breakdown.platformServiceFee(), 0.0001);
    }

    @Test
    void longDistanceStandardUsesDedicatedPricingRegime() {
        PackageDTO packageDTO = buildPackage("MEDIUM", 40, 30, 30, 10, "STANDARD", true, 50);

        PackagePricingBreakdown breakdown = PackageDeliveryPriceCalculator.calculatePricingBreakdown(470.0, packageDTO);

        assertEquals(26.8, breakdown.customerTotalPrice(), 0.0001);
        assertEquals(24.24, breakdown.deliveryBaseAmount(), 0.0001);
        assertEquals(1.0, breakdown.insuranceFee(), 0.0001);
        assertEquals(1.51, breakdown.platformServiceFee(), 0.0001);
        assertEquals("v4", breakdown.pricingVersion());
    }

    @Test
    void pricingRegimeSwitchesAfterOneHundredKilometers() {
        PackageDTO packageDTO = buildPackage("MEDIUM", 40, 30, 30, 10, "STANDARD", false, 0);

        PackagePricingBreakdown atThreshold = PackageDeliveryPriceCalculator.calculatePricingBreakdown(100.0, packageDTO);
        PackagePricingBreakdown aboveThreshold = PackageDeliveryPriceCalculator.calculatePricingBreakdown(100.1, packageDTO);

        assertEquals(31.0, atThreshold.customerTotalPrice(), 0.0001);
        assertEquals(19.6, aboveThreshold.customerTotalPrice(), 0.0001);
        assertEquals(18.32, aboveThreshold.deliveryBaseAmount(), 0.0001);
    }

    @Test
    void longDistanceModeCoefficientsRemainOrdered() {
        PackageDTO standard = buildPackage("SMALL", 20, 20, 20, 2, "STANDARD", false, 0);
        PackageDTO express = buildPackage("SMALL", 20, 20, 20, 2, "EXPRESS", false, 0);
        PackageDTO sameDay = buildPackage("SMALL", 20, 20, 20, 2, "SAME_DAY", false, 0);

        assertEquals(16.4, PackageDeliveryPriceCalculator.calculateDeliveryPrice(150, standard), 0.0001);
        assertEquals(20.1, PackageDeliveryPriceCalculator.calculateDeliveryPrice(150, express), 0.0001);
        assertEquals(24.1, PackageDeliveryPriceCalculator.calculateDeliveryPrice(150, sameDay), 0.0001);
    }

    @Test
    void shortDistanceMinimumBandsAreApplied() {
        PackageDTO smallStandard = buildPackage("SMALL", 20, 20, 20, 2, "STANDARD", false, 0);

        assertEquals(9.9, PackageDeliveryPriceCalculator.calculateDeliveryPrice(9, smallStandard), 0.0001);
        assertEquals(12.9, PackageDeliveryPriceCalculator.calculateDeliveryPrice(15, smallStandard), 0.0001);
    }

    @Test
    void invalidInputsAreRejected() {
        PackageDTO validPackage = buildPackage("MEDIUM", 40, 30, 30, 10, "STANDARD", false, 0);

        assertThrows(IllegalArgumentException.class,
                () -> PackageDeliveryPriceCalculator.calculatePricingBreakdown(0, validPackage));
        assertThrows(IllegalArgumentException.class,
                () -> PackageDeliveryPriceCalculator.calculatePricingBreakdown(10, buildPackage("UNKNOWN", 40, 30, 30, 10, "STANDARD", false, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> PackageDeliveryPriceCalculator.calculatePricingBreakdown(10, buildPackage("MEDIUM", 40, 30, 30, 0, "STANDARD", false, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> PackageDeliveryPriceCalculator.calculatePricingBreakdown(10, buildPackage("MEDIUM", 40, 30, 30, 10, "UNKNOWN", false, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> PackageDeliveryPriceCalculator.calculatePricingBreakdown(10, buildPackage("MEDIUM", 40, 30, 30, 10, "STANDARD", true, 0)));
    }

    private PackageDTO buildPackage(String category,
                                    double height,
                                    double width,
                                    double depth,
                                    double weight,
                                    String speed,
                                    boolean insurance,
                                    double declaredValue) {
        PackageDTO packageDTO = new PackageDTO();
        packageDTO.setPackageSizeCategory(category);
        packageDTO.setHeight((float) height);
        packageDTO.setWidth((float) width);
        packageDTO.setDepth((float) depth);
        packageDTO.setWeight((float) weight);
        packageDTO.setDeliverySpeed(speed);
        packageDTO.setInsuranceSelected(insurance);
        packageDTO.setDeclaredValue(declaredValue);

        AddressDTO departure = new AddressDTO();
        departure.setLine1("3 Rue Pasteur");
        departure.setZipCode("94450");
        departure.setTown("Limeil-Brevannes");
        departure.setCountry("France");

        AddressDTO arrival = new AddressDTO();
        arrival.setLine1("22 Rue Bernard Dimey");
        arrival.setZipCode("75018");
        arrival.setTown("Paris");
        arrival.setCountry("France");

        packageDTO.setAddresses(List.of(departure, arrival));
        return packageDTO;
    }
}
