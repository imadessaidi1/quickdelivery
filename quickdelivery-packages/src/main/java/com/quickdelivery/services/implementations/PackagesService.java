package com.quickdelivery.services.implementations;

import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.entities.Address;
import com.quickdelivery.abstarct.entities.Package;
import com.quickdelivery.abstarct.helpers.GeoHelper;
import com.quickdelivery.abstarct.repositories.Packages;
import com.quickdelivery.abstarct.repositories.Users;
import com.quickdelivery.services.interfaces.IPackagesService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PackagesService implements IPackagesService {
    @Value("${mapquest.geocode.url.part1}")
    private String mapQuestURL1;
    @Value("${mapquest.geocode.url.part2}")
    private String mapQuestURL2;
    @Value("${mapquest.key}")
    private String mapQUestKey;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private Packages packages;
    @Autowired
    private Users users;
    @Override
    public PackageDTO createNewPackage(PackageDTO packageDTO) {
        createPackage(packageDTO);
        return packageDTO;
    }

    @Override
    public void createNewPackages(List<PackageDTO> packageDTOS) {
        packageDTOS.stream().forEach(packageDTO -> createPackage(packageDTO));
    }

    @Override
    public List<PackageDTO> getPAckagesAroundPosition(String latitude, String longitude, double rayonEnMetres) {
        List<Address> addresses = packages.findAddressAroundPosition(latitude,longitude,rayonEnMetres);
        List<PackageDTO> packageDTOS = new ArrayList<>();
        addresses.stream().forEach(address -> packageDTOS.add(modelMapper.map(address.getPackaged(), PackageDTO.class)));
        return packageDTOS;
    }

    @Override
    public List<PackageDTO> findAddressOnMyRoad(String departureLatitude, String arrivalLatitude, String departureLongitude, String arrivalLongitude) {
        if(departureLatitude.compareTo(arrivalLatitude) > 0){
            String permut = departureLatitude;
            departureLatitude = arrivalLatitude;
            arrivalLatitude = permut;
        }
        if(departureLongitude.compareTo(arrivalLongitude)>0){
            String permut = departureLongitude;
            departureLongitude = arrivalLongitude;
            arrivalLongitude = permut;
        }
        List<Package> packages = this.packages.findAddressOnMyRoad(departureLatitude,arrivalLatitude,departureLongitude,arrivalLongitude);
        List<PackageDTO> packageDTOS = new ArrayList<>();
        packages.stream().forEach(aPackage -> {
            if(aPackage.getAddresses().size()==2){
                packageDTOS.add(modelMapper.map(aPackage, PackageDTO.class));
            }});
        return packageDTOS;
    }

    @Override
    public PackageDTO findPackageByID(Long id) {
        return null;
    }

    @Override
    public PackageDTO updatePackage(PackageDTO user) {
        return null;
    }

    @Override
    public void deletePackage(PackageDTO user) {

    }

    private void createPackage(PackageDTO packageDTO){
        packageDTO.getAddresses().stream().forEach(addressDTO -> GeoHelper.AdressGeoCoding(addressDTO, mapQuestURL1, mapQUestKey, mapQuestURL2));
        Package aPackage = modelMapper.map(packageDTO, Package.class);
        aPackage.getAddresses().stream().forEach(address -> address.setPackaged(aPackage));
        aPackage.setSender(users.findById(packageDTO.getSenderID()).get());
        packages.save(aPackage);
        packageDTO.setId(aPackage.getId());
        packageDTO.setVersion(aPackage.getVersion());
    }
}
