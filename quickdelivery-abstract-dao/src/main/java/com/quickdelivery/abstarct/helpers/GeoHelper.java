package com.quickdelivery.abstarct.helpers;

import com.quickdelivery.abstarct.dto.AddressDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

public class GeoHelper {

    public static void AdressGeoCoding(AddressDTO addressDTO, String mapQuestURL1, String mapQUestKey, String mapQuestURL2){
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response
                = restTemplate.getForEntity(mapQuestURL1 + mapQUestKey + mapQuestURL2 +addressDTO.toString(), Map.class);
        String lat= ((Map)((Map)((ArrayList)((Map)((ArrayList)response.getBody().get("results")).get(0)).get("locations")).get(0)).get("latLng")).get("lat").toString();
        String lng= ((Map)((Map)((ArrayList)((Map)((ArrayList)response.getBody().get("results")).get(0)).get("locations")).get(0)).get("latLng")).get("lng").toString();
        addressDTO.setLatitude(new BigDecimal(lat));
        addressDTO.setLongitude(new BigDecimal(lng));
    }
}
