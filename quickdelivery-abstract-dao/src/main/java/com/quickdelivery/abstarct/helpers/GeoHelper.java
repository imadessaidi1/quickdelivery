package com.quickdelivery.abstarct.helpers;

import com.google.maps.DirectionsApi;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.*;
import com.quickdelivery.abstarct.dto.AddressDTO;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class GeoHelper {

    public static void AddressGeoCoding(GeoApiContext  geoApiContext, AddressDTO addressDTO) throws IOException, InterruptedException, ApiException {
        GeocodingResult[] results =  GeocodingApi.geocode(geoApiContext,
                addressDTO.toString()).await();
        addressDTO.setLatitude((new BigDecimal(results[0].geometry.location.lat)).setScale(8, RoundingMode.CEILING));
        addressDTO.setLongitude((new BigDecimal(results[0].geometry.location.lng)).setScale(8, RoundingMode.CEILING));
    }

    public static DistanceMatrix getDistanceByCoordinates(GeoApiContext  geoApiContext, double departureLat, double departureLng, double arrivalLat, double arrivalLng){
        return DistanceMatrixApi.newRequest(geoApiContext)
                .origins(new LatLng(departureLat, departureLng))
                .destinations(new LatLng(arrivalLat, arrivalLng))
                .mode(TravelMode.DRIVING)
                .awaitIgnoreError();
    }
    public static DistanceMatrix getDistanceByAddress(GeoApiContext  geoApiContext, String departure, String arrival){
        return DistanceMatrixApi.newRequest(geoApiContext)
                .origins(departure)
                .destinations(arrival)
                .mode(TravelMode.DRIVING)
                .awaitIgnoreError();
    }

    public static DirectionsResult getDirection(GeoApiContext  geoApiContext, String departure, String arrival){
        return DirectionsApi.newRequest(geoApiContext)
                .origin(departure)
                .destination(arrival)
                .mode(TravelMode.DRIVING)
                .awaitIgnoreError();
    }
}
