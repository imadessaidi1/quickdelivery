package com.quickdelivery.abstarct.helpers;

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
    public static double calculateDeliveryPrice(GeoApiContext context, PackageDTO packageDTO) {
        double basePrice = 10.0;
        double distance = calculateDistance(context, packageDTO.getAddresses());
        double weight = packageDTO.getWeight();
        double volumetricWeight = calculateVolumetricWeight(packageDTO.getDepth(), packageDTO.getWidth(), packageDTO.getHeight(),weight);
        double additionalFees = calculateAdditionalFees(distance, weight, volumetricWeight);
        double totalPrice = basePrice + additionalFees;

        return totalPrice;
    }

    private static double calculateDistance(GeoApiContext context, List<AddressDTO> addresses) {
        DirectionsResult result = null;
        try {
            result = DirectionsApi.newRequest(context)
                    .origin(addresses.get(0).toString())
                    .destination(addresses.get(1).toString())
                    .mode(TravelMode.DRIVING)
                    .await();
            long distanceInMeters = result.routes[0].legs[0].distance.inMeters;
            return distanceInMeters / 1000.0;
        } catch (ApiException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static double calculateVolumetricWeight(double length, double width, double height, double weight) {
        double volume = length * width * height;
        double densityFactor = weight/volume;
        return (length * width * height)/densityFactor;
    }

    private static double calculateAdditionalFees(double distance, double weight, double volumetricWeight) {
        return (0.1 * distance)+(2 * weight)+(0 * volumetricWeight);
    }
}
