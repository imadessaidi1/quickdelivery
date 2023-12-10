package com.quickdelivery.controllers;

import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.helpers.PackegeCSVReader;
import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;
import com.quickdelivery.services.interfaces.IPackagesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/packages/v1")
public class PackageController {
    @Autowired
    private IPackagesService packagesService;
    @PostMapping("/create")
    public PackageDTO createNewPackage(@RequestBody PackageDTO packageDTO){
        return packagesService.createNewPackage(packageDTO);
    }

    @PostMapping("/bulk-create")
    public void createNewPackages(@RequestBody List<PackageDTO> packageDTOS){
        packagesService.createNewPackages(packageDTOS);
    }

    @GetMapping("/packages-around{latitude}{longitude}{rayonEnMetres}")
    public List<PackageDTO> packagesAroundPosition(@RequestParam(name = "latitude", required = true) String latitude,
                                                    @RequestParam(name = "longitude", required = true) String longitude,
                                                    @RequestParam(name = "rayonEnMetres", required = true) double rayonEnMetres){
        System.out.println("test");
        return packagesService.getPAckagesAroundPosition(latitude,longitude,rayonEnMetres);
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
}
