package com.quickdelivery.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickdelivery.abstarct.dto.AddressDTO;
import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.dto.PositionDTO;
import com.quickdelivery.abstarct.helpers.PackegeCSVReader;
import com.quickdelivery.abstarct.parameters.CHECK_STATUS;
import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;
import com.quickdelivery.services.interfaces.IPackagesService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/packages/v1")
public class PackageController {
    @Autowired
    private IPackagesService packagesService;
    @Autowired
    private ModelMapper modelMapper;
    @PostMapping("/create")
    public PackageDTO createNewPackage(@RequestParam("packageDTO") String packageDTO,
                                       @RequestParam("files") MultipartFile[] files){
        ObjectMapper objectMapper = new ObjectMapper();
        PackageDTO packageDTO1 = null;
        try {
            packageDTO1 = objectMapper.readValue(packageDTO, PackageDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return packagesService.createNewPackage(packageDTO1, files);
    }

    @PostMapping("/bulk-create")
    public void createNewPackages(@RequestBody List<PackageDTO> packageDTOS){
        packagesService.createNewPackages(packageDTOS);
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
    public void createNewPackagesFromCSV(@RequestParam("file") MultipartFile file){
        List<PackageDTO> packageDTOList = PackegeCSVReader.CSVToPackages(file);
        packagesService.createNewPackages(packageDTOList);
    }

    @PutMapping("/update-packages-status")
    public void updatePackageStatus(@RequestParam Map<String, String> requestMap){
        requestMap.forEach((id, status) -> packagesService.updatePackageStatus(PACKAGE_STATUS.valueOf(status),Long.valueOf(id)));
    }

    @PutMapping("/reserve{packageID}{deliveryPersonID}")
    public void reservePackage(@RequestParam("packageID") Long packageID,
                               @RequestParam("deliveryPersonID") Long deliveryPersonID){
        try {
            packagesService.reservePackage(packageID,deliveryPersonID);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/pickup{packageID}{deliveryPersonID}{pickUpOTP}")
    public void pickUpPackage(@RequestParam("packageID") Long packageID,
                              @RequestParam("deliveryPersonID") Long deliveryPersonID,
                              @RequestParam("pickUpOTP") String pickUpOTP){
        try {
            packagesService.pickUpPackage(packageID,deliveryPersonID,pickUpOTP);
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
}
