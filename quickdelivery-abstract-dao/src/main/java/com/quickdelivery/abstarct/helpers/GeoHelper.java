package com.quickdelivery.abstarct.helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.quickdelivery.abstarct.dto.AddressDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

public class GeoHelper {

    public static void AdressGeoCoding(GeoApiContext  geoApiContext, AddressDTO addressDTO, String mapQuestURL1, String mapQUestKey, String mapQuestURL2) throws IOException, InterruptedException, ApiException {
        GeocodingResult[] results =  GeocodingApi.geocode(geoApiContext,
                addressDTO.toString()).await();
        geoApiContext.shutdown();
        addressDTO.setLatitude(new BigDecimal(results[0].geometry.location.lat));
        addressDTO.setLongitude(new BigDecimal(results[0].geometry.location.lng));
    }
}
