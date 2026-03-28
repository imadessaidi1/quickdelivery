package com.quickdelivery.helpers;

import com.quickdelivery.abstarct.dto.AddressDTO;
import com.quickdelivery.abstarct.dto.PackageDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackageDeliveryPriceCalculatorTest {

    @Test
    void standardDistance1kmReturns710() {
        PackageDTO packageDTO = buildPackage(20, 20, 20, 1, "STANDARD", false, 0, 0,
                false, false, 0);

        double price = PackageDeliveryPriceCalculator.calculateDeliveryPrice(1.0, packageDTO);

        assertEquals(7.1, price, 0.0001);
    }

    @Test
    void standardDistance22kmReturns850() {
        PackageDTO packageDTO = buildPackage(20, 20, 20, 1, "STANDARD", false, 0, 0,
                false, false, 0);

        double price = PackageDeliveryPriceCalculator.calculateDeliveryPrice(2.2, packageDTO);

        assertEquals(8.5, price, 0.0001);
    }

    @Test
    void standardDistance3kmReturns940() {
        PackageDTO packageDTO = buildPackage(20, 20, 20, 1, "STANDARD", false, 0, 0,
                false, false, 0);

        double price = PackageDeliveryPriceCalculator.calculateDeliveryPrice(3.0, packageDTO);

        assertEquals(9.4, price, 0.0001);
    }

    @Test
    void standardDistance31kmReturns950() {
        PackageDTO packageDTO = buildPackage(20, 20, 20, 1, "STANDARD", false, 0, 0,
                false, false, 0);

        double price = PackageDeliveryPriceCalculator.calculateDeliveryPrice(3.1, packageDTO);

        assertEquals(9.5, price, 0.0001);
    }

    @Test
    void standardDistance6kmReturns1230() {
        PackageDTO packageDTO = buildPackage(20, 20, 20, 1, "STANDARD", false, 0, 0,
                false, false, 0);

        double price = PackageDeliveryPriceCalculator.calculateDeliveryPrice(6.0, packageDTO);

        assertEquals(12.3, price, 0.0001);
    }

    @Test
    void standardDistance8kmReturns1440() {
        PackageDTO packageDTO = buildPackage(20, 20, 20, 1, "STANDARD", false, 0, 0,
                false, false, 0);

        double price = PackageDeliveryPriceCalculator.calculateDeliveryPrice(8.0, packageDTO);

        assertEquals(14.4, price, 0.0001);
    }

    @Test
    void standardDistance10kmReturns1640() {
        PackageDTO packageDTO = buildPackage(20, 20, 20, 1, "STANDARD", false, 0, 0,
                false, false, 0);

        double price = PackageDeliveryPriceCalculator.calculateDeliveryPrice(10.0, packageDTO);

        assertEquals(16.4, price, 0.0001);
    }

    @Test
    void standardDistance101kmReturns1650() {
        PackageDTO packageDTO = buildPackage(20, 20, 20, 1, "STANDARD", false, 0, 0,
                false, false, 0);

        double price = PackageDeliveryPriceCalculator.calculateDeliveryPrice(10.1, packageDTO);

        assertEquals(16.5, price, 0.0001);
    }

    @Test
    void standardDistance15kmReturns2070() {
        PackageDTO packageDTO = buildPackage(20, 20, 20, 1, "STANDARD", false, 0, 0,
                false, false, 0);

        double price = PackageDeliveryPriceCalculator.calculateDeliveryPrice(15.0, packageDTO);

        assertEquals(20.7, price, 0.0001);
    }

    @Test
    void expressAndSameDayAreMoreExpensiveThanStandardAt6km() {
        PackageDTO standard = buildPackage(40, 30, 30, 4, "STANDARD", false, 0, 0,
                false, false, 0);
        PackageDTO express = buildPackage(40, 30, 30, 4, "EXPRESS", false, 0, 0,
                false, false, 0);
        PackageDTO sameDay = buildPackage(40, 30, 30, 4, "SAMEDAY", false, 0, 0,
                false, false, 0);

        double standardPrice = PackageDeliveryPriceCalculator.calculateDeliveryPrice(6, standard);
        double expressPrice = PackageDeliveryPriceCalculator.calculateDeliveryPrice(6, express);
        double sameDayPrice = PackageDeliveryPriceCalculator.calculateDeliveryPrice(6, sameDay);

        assertEquals(15.7, standardPrice, 0.0001);
        assertEquals(20.0, expressPrice, 0.0001);
        assertEquals(24.2, sameDayPrice, 0.0001);
        assertTrue(expressPrice > standardPrice);
        assertTrue(sameDayPrice > expressPrice);
    }

    @Test
    void insuranceUsesDeclaredValueAndMinimumFee() {
        PackageDTO lowValuePackage = buildPackage(40, 30, 20, 3, "STANDARD", true, 0, 0,
                false, false, 50);
        PackageDTO highValuePackage = buildPackage(40, 30, 20, 3, "STANDARD", true, 0, 0,
                false, false, 500);

        double lowValuePrice = PackageDeliveryPriceCalculator.calculateDeliveryPrice(6, lowValuePackage);
        double highValuePrice = PackageDeliveryPriceCalculator.calculateDeliveryPrice(6, highValuePackage);

        assertEquals(15.7, lowValuePrice, 0.0001);
        assertEquals(24.9, highValuePrice, 0.0001);
        assertTrue(highValuePrice > lowValuePrice);
    }

    @Test
    void volumetricWeightCanDominateActualWeight() {
        PackageDTO smallDensePackage = buildPackage(20, 20, 20, 3, "STANDARD", false, 0, 0,
                false, false, 0);
        PackageDTO bulkyLightPackage = buildPackage(80, 60, 60, 3, "STANDARD", false, 0, 0,
                false, false, 0);

        double compactPrice = PackageDeliveryPriceCalculator.calculateDeliveryPrice(6, smallDensePackage);
        double bulkyPrice = PackageDeliveryPriceCalculator.calculateDeliveryPrice(6, bulkyLightPackage);

        assertTrue(bulkyPrice > compactPrice);
        assertEquals(63.8, bulkyPrice, 0.0001);
    }

    @Test
    void stairsWithoutElevatorCostMoreThanWithElevatorAndAreCapped() {
        PackageDTO withElevator = buildPackage(40, 30, 30, 6, "STANDARD", false, 4, 3,
                true, true, 0);
        PackageDTO withoutElevator = buildPackage(40, 30, 30, 6, "STANDARD", false, 4, 3,
                false, false, 0);
        PackageDTO cappedStairs = buildPackage(40, 30, 30, 20, "STANDARD", false, 8, 8,
                false, false, 0);

        double withElevatorPrice = PackageDeliveryPriceCalculator.calculateDeliveryPrice(6, withElevator);
        double withoutElevatorPrice = PackageDeliveryPriceCalculator.calculateDeliveryPrice(6, withoutElevator);
        double cappedPrice = PackageDeliveryPriceCalculator.calculateDeliveryPrice(6, cappedStairs);

        assertTrue(withoutElevatorPrice > withElevatorPrice);
        assertEquals(19.8, withElevatorPrice, 0.0001);
        assertEquals(22.2, withoutElevatorPrice, 0.0001);
        assertEquals(33.7, cappedPrice, 0.0001);
    }

    @Test
    void negativeOrNullFloorsDoNotAddCharges() {
        PackageDTO invalidFloors = buildPackage(40, 30, 30, 5, "STANDARD", false, -2, -1,
                false, false, 0);

        double price = PackageDeliveryPriceCalculator.calculateDeliveryPrice(6, invalidFloors);

        assertEquals(15.7, price, 0.0001);
    }

    private PackageDTO buildPackage(double height,
                                    double width,
                                    double depth,
                                    double weight,
                                    String speed,
                                    boolean insurance,
                                    int departureFloor,
                                    int arrivalFloor,
                                    boolean departureElevator,
                                    boolean arrivalElevator,
                                    double declaredValue) {
        PackageDTO packageDTO = new PackageDTO();
        packageDTO.setHeight((float) height);
        packageDTO.setWidth((float) width);
        packageDTO.setDepth((float) depth);
        packageDTO.setWeight((float) weight);
        packageDTO.setDeliverySpeed(speed);
        packageDTO.setInsuranceSelected(insurance);
        packageDTO.setDeclaredValue(declaredValue);

        AddressDTO departure = new AddressDTO();
        departure.setFloor(departureFloor);
        departure.setHasElevator(departureElevator);
        departure.setLine1("3 Rue Pasteur");
        departure.setZipCode("94450");
        departure.setTown("Limeil-Brevannes");
        departure.setCountry("France");

        AddressDTO arrival = new AddressDTO();
        arrival.setFloor(arrivalFloor);
        arrival.setHasElevator(arrivalElevator);
        arrival.setLine1("25 Avenue de la République");
        arrival.setZipCode("94700");
        arrival.setTown("Maisons-Alfort");
        arrival.setCountry("France");

        packageDTO.setAddresses(List.of(departure, arrival));
        return packageDTO;
    }
}
