package com.quickdelivery.services.interfaces;

import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.abstarct.entities.Address;
import org.apache.juli.logging.Log;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IPackagesService {
    public PackageDTO createNewPackage(PackageDTO packageDTO);
    public void createNewPackages(List<PackageDTO> packageDTOS);
    public List<PackageDTO> getPAckagesAroundPosition(String latitude, String longitude,double rayonEnMetres);
    public List<PackageDTO> findAddressOnMyRoad(String departureLatitude, String arrivalLatitude, String departureLongitude, String arrivalLongitude);
    public PackageDTO findPackageByID(Long id);
    public PackageDTO updatePackage(PackageDTO packageDTO);
    public void deletePackage(PackageDTO packageDTO);
}
