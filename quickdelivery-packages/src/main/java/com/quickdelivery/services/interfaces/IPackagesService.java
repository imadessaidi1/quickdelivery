package com.quickdelivery.services.interfaces;

import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.dto.UserDTO;
import com.quickdelivery.abstarct.entities.Address;
import com.quickdelivery.abstarct.entities.Package;
import com.quickdelivery.abstarct.parameters.CHECK_STATUS;
import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;
import org.apache.juli.logging.Log;
import org.springframework.data.repository.query.Param;

import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface IPackagesService {
    public PackageDTO createNewPackage(PackageDTO packageDTO);
    void createNewPackages(List<PackageDTO> packageDTOS);
    List<PackageDTO> getPAckagesAroundPosition(String latitude, String longitude,double rayonEnMetres);
    List<PackageDTO> findAddressOnMyRoad(String departureLatitude, String arrivalLatitude, String departureLongitude, String arrivalLongitude);
    void reservePackage(Long packageID, Long deliveryPersonID) throws NoSuchAlgorithmException;
    void pickUpPackage(Long packageID, Long deliveryPersonID, String pickUpOTP) throws NoSuchAlgorithmException;
    CHECK_STATUS checkOTPForPickUpPackage(Long packageID, Long senderID, String pickUpOTP);
    CHECK_STATUS checkOTPForDeliverPackage(Long packageID, Long deliveryPersonID, String pickUpOTP);
    PackageDTO findPackageByID(Long id);
    PackageDTO updatePackage(PackageDTO packageDTO);
    void deletePackage(PackageDTO packageDTO);
    List<PackageDTO> findPackagesByStatus(PACKAGE_STATUS status);

    void updatePackageStatus(PACKAGE_STATUS status, Long id);
}
