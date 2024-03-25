package com.quickdelivery.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickdelivery.abstarct.dto.MessageDTO;
import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.entities.Address;
import com.quickdelivery.abstarct.entities.PackageReservation;
import com.quickdelivery.abstarct.helpers.PackegeCSVReader;
import com.quickdelivery.abstarct.parameters.CHECK_STATUS;
import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;
import com.quickdelivery.config.WebSocketHandler;
import com.quickdelivery.services.interfaces.IPackagesService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@EnableAspectJAutoProxy
@RequestMapping("/packages/v1")
public class PackageController {
    @Autowired
    private IPackagesService packagesService;
    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    WebSocketHandler webSocketHandler;

    @PostMapping("/create")
    public PackageDTO createNewPackage(@RequestParam("packageDTO") String packageDTO,
                                       @RequestParam(value = "files", required = false) MultipartFile[] files,
                                       @RequestParam("locale") Locale locale){
        ObjectMapper objectMapper = new ObjectMapper();
        PackageDTO packageDTO1 = null;
        try {
            packageDTO1 = objectMapper.readValue(packageDTO, PackageDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        PackageDTO aPackage = packagesService.createNewPackage(packageDTO1, files, locale);
        notifyPackageCreation(aPackage.getReference());
        return  aPackage;
    }

    @PostMapping("/bulk-create")
    public void createNewPackages(@RequestBody List<PackageDTO> packageDTOS, Locale locale){
        packagesService.createNewPackages(packageDTOS, locale);
    }

    @GetMapping("/packages-around{latitude}{longitude}{rayonEnMetres}")
    @Cacheable(value="PackagesAroundMe", keyGenerator="customKeyGenerator")
    public Map<String, List<PackageDTO>> packagesAroundPosition(@RequestParam(name = "latitude", required = true) String latitude,
                                                                    @RequestParam(name = "longitude", required = true) String longitude,
                                                                    @RequestParam(name = "rayonEnMetres", required = true) double rayonEnMetres){
        return packagesService.getPAckagesAroundPosition(latitude,longitude,rayonEnMetres);
    }

    @GetMapping("/packages-around-me{latitude}{longitude}{rayonEnMetres}")
    public List<PackageDTO> packagesAroundMyPosition(@RequestParam(name = "latitude", required = true) String latitude,
                                                                @RequestParam(name = "longitude", required = true) String longitude,
                                                                @RequestParam(name = "rayonEnMetres", required = true) double rayonEnMetres){
        return packagesService.getPackagesAroundPosition(latitude,longitude,rayonEnMetres);
    }

    @GetMapping("/packages-on-my-road{departureLatitude}{departureLongitude}{arrivalLatitude}{arrivalLongitude}")
    public List<PackageDTO> findPackagesOnMyRoad(@RequestParam(name = "departureLatitude", required = true) String departureLatitude,
                                                   @RequestParam(name = "arrivalLatitude", required = true) String arrivalLatitude,
                                                 @RequestParam(name = "departureLongitude", required = true) String departureLongitude,
                                                 @RequestParam(name = "arrivalLongitude", required = true) String arrivalLongitude){
        return packagesService.findAddressOnMyRoad(departureLatitude,arrivalLatitude,departureLongitude, arrivalLongitude);
    }

    @GetMapping("/package-by-status{status}")
    public List<PackageDTO> findPackagesByStatus(@RequestParam(name = "status", required = true) PACKAGE_STATUS status){
        return packagesService.findPackagesByStatus(status);
    }

    @PostMapping("/upload")
    public void createNewPackagesFromCSV(@RequestParam("file") MultipartFile file, Locale locale){
        List<PackageDTO> packageDTOList = PackegeCSVReader.CSVToPackages(file);
        packagesService.createNewPackages(packageDTOList, locale);
    }

    @PutMapping("/update-packages-status")
    public void updatePackageStatus(@RequestParam Map<String, String> requestMap){
        requestMap.forEach((packageId, packageStatus) -> {
            packagesService.updatePackageStatus(PACKAGE_STATUS.valueOf(packageStatus),Long.valueOf(packageId));
        });
    }

    @PutMapping("/reserve{packageID}{deliveryPersonID}{locale}")
    public void reservePackage(@RequestParam("packageID") Long packageID,
                               @RequestParam("deliveryPersonID") Long deliveryPersonID,
                               @RequestParam("locale") Locale locale){
        try {
            PackageReservation packageReservation = packagesService.reservePackage(packageID, deliveryPersonID, locale);
            notifyPackageReservation(packageReservation.getPickUpOTP(), deliveryPersonID, packageReservation.getaPackage().getReference());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/pickup{packageID}{deliveryPersonID}{pickUpOTP}{locale}")
    public void pickUpPackage(@RequestParam("packageID") Long packageID,
                              @RequestParam("deliveryPersonID") Long deliveryPersonID,
                              @RequestParam("pickUpOTP") String pickUpOTP,
                              @RequestParam("locale") Locale locale){
        try {
            packagesService.pickUpPackage(packageID,deliveryPersonID,pickUpOTP,locale);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/deliver{packageID}{deliveryPersonID}{pickUpOTP}{locale}")
    public void deliverPackage(@RequestParam("packageID") Long packageID,
                              @RequestParam("deliveryPersonID") Long deliveryPersonID,
                              @RequestParam("deliveryOTP") String deliveryOTP,
                              @RequestParam("locale") Locale locale){
        try {
            packagesService.deliverPackage(packageID,deliveryPersonID,deliveryOTP,locale);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/checkOTPForPickup{packageID}{senderID}{pickUpOTP}")
    public CHECK_STATUS checkOTPForPickUpPackage(@RequestParam("packageID") Long packageID,
                                                 @RequestParam("senderID") Long senderID,
                                                 @RequestParam("pickUpOTP") String pickUpOTP){
            return packagesService.checkOTPForPickUpPackage(packageID,senderID,pickUpOTP);
    }

    @GetMapping("/checkOTPForDelivery{packageID}{deliveryPersonID}{deliveryOTP}")
    public CHECK_STATUS checkOTPForDeliveryPackage(@RequestParam("packageID") Long packageID,
                                           @RequestParam("deliveryPersonID") Long deliveryPersonID,
                                           @RequestParam("deliveryOTP") String deliveryOTP){
        return packagesService.checkOTPForPickUpPackage(packageID,deliveryPersonID,deliveryOTP);
    }
    @GetMapping("/getPackagesByDeliveryPerson{deliveryPersonID}")
    public Map<PACKAGE_STATUS, List<PackageDTO>> getPackagesByDeliveryPerson(@RequestParam("deliveryPersonID") Long deliveryPersonID){
        return packagesService.getPackagesByDeliveryPerson(deliveryPersonID);
    }

    @GetMapping("/getPackage{reference}")
    public PackageDTO getPackagesByID(@RequestParam("reference") String reference){
        return packagesService.findPackageByReference(reference);
    }

    @GetMapping("/notify")
    public void notifyClient(){
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setFrom("PACKAGE_SERVICE");
        messageDTO.setType("NEW_PACKAGE_NOTIFICATION");
        messageDTO.setTo("1652");
        messageDTO.setMessage("There is a new package around you :)");
        ObjectMapper objectMapper = new ObjectMapper();
        String json;
        try {
            json = objectMapper.writeValueAsString(messageDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        webSocketHandler.sendMessageToAll(json);
    }

    @GetMapping("/isUserWithOngoingDelivery{userId}")
    public Boolean isUserWithOngoingDelivery(@RequestParam("userId") Long userId){
        return packagesService.isUserWithOngoingDelivery(userId);
    }
    private void notifyPackageCreation(String aPackage){
        List<Address> addressAroundNewPackage = packagesService.findUsersAroundPosition(aPackage);
        for (Address address : addressAroundNewPackage){
            MessageDTO messageDTO = new MessageDTO();
            messageDTO.setFrom("PACKAGE_SERVICE");
            messageDTO.setType("NEW_PACKAGE_NOTIFICATION");
            messageDTO.setTo(address.getResidents().getId().toString());
            messageDTO.setMessage("There is a new package around you :)");
            messageDTO.setUrl("https://quickdelivery.com:8080/package/"+aPackage);
            ObjectMapper objectMapper = new ObjectMapper();
            String json;
            try {
                json = objectMapper.writeValueAsString(messageDTO);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            webSocketHandler.sendMessageToAll(json);
        };
    }

    private void notifyPackageReservation(String otp, Long userID, String packageReference){
            MessageDTO messageDTO = new MessageDTO();
            messageDTO.setFrom("PACKAGE_SERVICE");
            messageDTO.setType("PACKAGE_RESERVATION_OTP_NOTIFICATION");
            messageDTO.setTo(userID.toString());
            messageDTO.setMessage("You reserved a package. Here is the password to pick it up : "+otp);
            messageDTO.setUrl("http://localhost:8080/package/"+packageReference);
            ObjectMapper objectMapper = new ObjectMapper();
            String json;
            try {
                json = objectMapper.writeValueAsString(messageDTO);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            webSocketHandler.sendMessageToAll(json);
    }
}
