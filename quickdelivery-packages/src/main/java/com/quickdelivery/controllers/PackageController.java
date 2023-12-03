package com.quickdelivery.controllers;

import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.helpers.PackegeCSVReader;
import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;
import com.quickdelivery.services.interfaces.IPackagesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
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
        return packagesService.getPAckagesAroundPosition(latitude,longitude,rayonEnMetres);
    }

    @GetMapping("/packages-on-my-road{departureLatitude}{departureLongitude}{ArrivalLatitude}{ArrivalLongitude}")
    public List<PackageDTO> findPackagesOnMyRoad(@RequestParam(name = "departureLatitude", required = true) String departureLatitude,
                                                   @RequestParam(name = "ArrivalLatitude", required = true) String arrivalLatitude,
                                                 @RequestParam(name = "departureLongitude", required = true) String departureLongitude,
                                                 @RequestParam(name = "arrivalLongitude", required = true) String arrivalLongitude){
        return packagesService.findAddressOnMyRoad(departureLatitude,arrivalLatitude,departureLongitude, arrivalLongitude);
    }
    @GetMapping("/package{id}")
    public PackageDTO findUserById(@RequestParam(name = "id", required = true) Long id){
        return packagesService.findPackageByID(id);
    }

    @GetMapping("/package-by-status{status}")
    public List<PackageDTO> findUserById(@RequestParam(name = "status", required = true) PACKAGE_STATUS status){
        return packagesService.findPackagesByStatus(status);
    }

    @PostMapping("/upload")
    public void createNewPackagesFromCSV(@RequestParam("file") MultipartFile file){
        List<PackageDTO> packageDTOList = PackegeCSVReader.CSVToPackages(file);
        packagesService.createNewPackages(packageDTOList);
    }

    @PutMapping("/update-package-status{status}{id}")
    public void updatePackageStatus(@RequestParam(name = "status", required = true) PACKAGE_STATUS status,
                                    @RequestParam(name = "id", required = true) Long id){
        packagesService.updatePackageStatus(status,id);
    }
}
