package com.quickdelivery.controllers;

import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.services.interfaces.IPackagesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

}
