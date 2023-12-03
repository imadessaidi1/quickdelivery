package com.quickdelivery.services.interfaces;

import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.abstarct.entities.Address;
import com.quickdelivery.abstarct.entities.Package;
import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;
import org.apache.juli.logging.Log;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IPackagesService {
    public PackageDTO createNewPackage(PackageDTO packageDTO);
    void createNewPackages(List<PackageDTO> packageDTOS);
    List<PackageDTO> getPAckagesAroundPosition(String latitude, String longitude,double rayonEnMetres);
    List<PackageDTO> findAddressOnMyRoad(String departureLatitude, String arrivalLatitude, String departureLongitude, String arrivalLongitude);
    PackageDTO findPackageByID(Long id);
    PackageDTO updatePackage(PackageDTO packageDTO);
    void deletePackage(PackageDTO packageDTO);
    List<PackageDTO> findPackagesByStatus(PACKAGE_STATUS status);

    void updatePackageStatus(PACKAGE_STATUS status, Long id);
}
