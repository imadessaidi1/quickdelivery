package com.quickdelivery.controllers;

import com.quickdelivery.abstarct.dto.PackageDTO;
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

}
