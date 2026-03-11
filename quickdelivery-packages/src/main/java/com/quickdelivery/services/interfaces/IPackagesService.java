package com.quickdelivery.services.interfaces;

import com.google.maps.errors.ApiException;
import com.quickdelivery.abstarct.dto.AddressDTO;
import com.quickdelivery.abstarct.dto.MessageDTO;
import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.dto.PositionDTO;
import com.quickdelivery.abstarct.entities.Address;
import com.quickdelivery.abstarct.entities.PackageReservation;
import com.quickdelivery.abstarct.parameters.CHECK_STATUS;
import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;
import com.quickdelivery.dto.ReserveBatchResultDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface IPackagesService {
    public PackageDTO createNewPackage(PackageDTO packageDTO, MultipartFile[] files, Locale locale);
    void createNewPackages(List<PackageDTO> packageDTOS, Locale locale);
    Map<String, List<PackageDTO>> getPAckagesAroundPosition(String latitude, String longitude, double rayonEnMetres);

    Map<String, List<PackageDTO>> getPAckagesAroundPosition(AddressDTO address, double rayonEnMetres) throws IOException, InterruptedException, ApiException;

    List<PackageDTO> getPackagesAroundPosition(String latitude, String longitude, double rayonEnMetres);
    List<PackageDTO> findAddressOnMyRoad(String departureLatitude, String arrivalLatitude, String departureLongitude, String arrivalLongitude);
    PackageReservation reservePackage(Long packageID, Long deliveryPersonID, Locale locale) throws NoSuchAlgorithmException;
    ReserveBatchResultDTO reservePackagesBatch(List<Long> packageIds, Long deliveryPersonID, Locale locale);
    void pickUpPackage(Long packageID, Long deliveryPersonID, String pickUpOTP, Locale locale) throws NoSuchAlgorithmException;
    void deliverPackage(Long packageID, Long deliveryPersonID, String pickUpOTP, Locale locale) throws NoSuchAlgorithmException;
    CHECK_STATUS checkOTPForPickUpPackage(Long packageID, Long senderID, String pickUpOTP);
    CHECK_STATUS checkOTPForDeliverPackage(Long packageID, Long deliveryPersonID, String pickUpOTP);
    CHECK_STATUS checkGuestOTPForPickUpPackage(Long packageID, String guestAccessToken, String pickUpOTP);
    CHECK_STATUS checkGuestOTPForDeliverPackage(Long packageID, String guestAccessToken, String deliveryOTP);
    void confirmGuestPackagePayment(Long packageID, String guestAccessToken);
    PackageDTO findPackageByID(Long id);
    PackageDTO updatePackage(PackageDTO packageDTO);
    void deletePackage(PackageDTO packageDTO);
    List<PackageDTO> findPackagesByStatus(PACKAGE_STATUS status);

    void updatePackageStatus(PACKAGE_STATUS status, Long id);

    Map<PACKAGE_STATUS, List<PackageDTO>> getPackagesByDeliveryPerson(Long deliveryPersonID);

    List<Address> findUsersAroundPosition(String aPackage);

    PackageDTO findPackageByReference(String reference);
    PackageDTO findGuestPackageByReference(String reference, String guestAccessToken);

    boolean isUserWithOngoingDelivery(Long usedId);

    Map<String, PositionDTO> handleWebsocketMessage(MessageDTO messageDTO);
}
