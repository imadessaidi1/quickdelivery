package com.quickdelivery.services.interfaces;

import com.google.maps.errors.ApiException;
import com.quickdelivery.abstarct.dto.AddressDTO;
import com.quickdelivery.abstarct.dto.ActiveTrackingPackageDTO;
import com.quickdelivery.abstarct.dto.AdminPackageDashboardSummaryDTO;
import com.quickdelivery.abstarct.dto.DeliveryReservationContextDTO;
import com.quickdelivery.abstarct.dto.DocumentContentDTO;
import com.quickdelivery.abstarct.dto.CourierPenaltyDTO;
import com.quickdelivery.abstarct.dto.FinancialDashboardDTO;
import com.quickdelivery.abstarct.dto.MessageDTO;
import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.dto.PositionDTO;
import com.quickdelivery.abstarct.dto.ServiceMetricsDTO;
import com.quickdelivery.abstarct.dto.ServiceHttpBreakdownDTO;
import com.quickdelivery.abstarct.entities.Address;
import com.quickdelivery.abstarct.entities.PackageReservation;
import com.quickdelivery.abstarct.parameters.CHECK_STATUS;
import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;
import com.quickdelivery.dto.ReserveBatchResultDTO;
import com.quickdelivery.dto.ReserveBatchPlanRequestDTO;
import com.quickdelivery.dto.ReservationAvailabilityDTO;
import com.quickdelivery.dto.RoutePlanDTO;
import com.quickdelivery.dto.RoutePlanRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public interface IPackagesService {
    public PackageDTO createNewPackage(PackageDTO packageDTO, MultipartFile[] files, Locale locale);
    PackageDTO estimateDeliveryPrice(PackageDTO packageDTO);
    void createNewPackages(List<PackageDTO> packageDTOS, Locale locale);
    Map<String, List<PackageDTO>> getPAckagesAroundPosition(String latitude, String longitude, double rayonEnMetres);
    Map<String, List<PackageDTO>> getPAckagesAroundPosition(String latitude, String longitude, double rayonEnMetres, String deliveryMode);

    Map<String, List<PackageDTO>> getPAckagesAroundPosition(AddressDTO address, double rayonEnMetres) throws IOException, InterruptedException, ApiException;

    List<PackageDTO> getPackagesAroundPosition(String latitude, String longitude, double rayonEnMetres);
    List<PackageDTO> getPackagesAroundPosition(String latitude, String longitude, double rayonEnMetres, String deliveryMode);
    List<PackageDTO> getPackagesInBounds(double minLat, double maxLat, double minLng, double maxLng, double centerLat, double centerLng, int limit);
    List<PackageDTO> getPackagesInBounds(double minLat, double maxLat, double minLng, double maxLng, double centerLat, double centerLng, int limit, String deliveryMode);
    List<PackageDTO> getPackagesAroundPositionWithDestination(String latitude, String longitude, AddressDTO destinationAddress, double rayonEnMetres) throws IOException, InterruptedException, ApiException;
    List<PackageDTO> getPackagesAroundPositionWithDestination(String latitude, String longitude, AddressDTO destinationAddress, double rayonEnMetres, String deliveryMode) throws IOException, InterruptedException, ApiException;
    List<PackageDTO> getPackagesAroundPositionWithDestination(String latitude, String longitude, AddressDTO destinationAddress, double rayonEnMetres, String deliveryMode, String vehicleType) throws IOException, InterruptedException, ApiException;
    RoutePlanDTO buildRoutePlan(RoutePlanRequestDTO request);
    List<PackageDTO> findAddressOnMyRoad(String departureLatitude, String arrivalLatitude, String departureLongitude, String arrivalLongitude);
    List<PackageDTO> findAddressOnMyRoad(String departureLatitude, String arrivalLatitude, String departureLongitude, String arrivalLongitude, String deliveryMode);
    List<PackageDTO> findAddressOnMyRoad(String departureLatitude, String arrivalLatitude, String departureLongitude, String arrivalLongitude, String deliveryMode, String vehicleType, double radiusMeters);
    PackageReservation reservePackage(Long packageID, Long deliveryPersonID, Locale locale) throws NoSuchAlgorithmException;
    ReserveBatchResultDTO reservePackagesBatch(List<Long> packageIds, Long deliveryPersonID, Locale locale);
    ReserveBatchResultDTO reservePackagesBatchFromPlan(ReserveBatchPlanRequestDTO request, Long deliveryPersonID, Locale locale);
    PackageDTO cancelReservation(Long packageID, Long deliveryPersonID);
    RoutePlanDTO cancelActiveDeliveryRoute(Long deliveryPersonId);
    void pickUpPackage(Long packageID, Long deliveryPersonID, String pickUpOTP, Locale locale, PositionDTO currentPosition) throws NoSuchAlgorithmException;
    void deliverPackage(Long packageID, Long deliveryPersonID, String pickUpOTP, Locale locale, PositionDTO currentPosition) throws NoSuchAlgorithmException;
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
    Map<PACKAGE_STATUS, List<PackageDTO>> getPackagesBySender(Long senderID);
    DeliveryReservationContextDTO getDeliveryReservationContext(Long packageId, Long deliveryPersonId);

    List<Address> findUsersAroundPosition(String aPackage);

    PackageDTO findPackageByReference(String reference);
    PackageDTO findGuestPackageByReference(String reference, String guestAccessToken);
    PackageDTO findTrackingSubscriptionPackage(String reference, String guestAccessToken);
    DocumentContentDTO loadPackageDocumentContent(Long documentId);

    boolean isUserWithOngoingDelivery(Long usedId);
    ReservationAvailabilityDTO getReservationAvailability(Long deliveryPersonId);
    ServiceMetricsDTO loadAdminMetrics();
    ServiceMetricsDTO loadAdminTrackingMetrics();
    ServiceHttpBreakdownDTO loadAdminHttpBreakdown();
    AdminPackageDashboardSummaryDTO loadAdminDashboardSummary(int year);
    FinancialDashboardDTO loadAdminFinancialDashboard();
    List<CourierPenaltyDTO> loadAdminCourierPenalties(int limit);
    RoutePlanDTO getActiveDeliveryRoute(Long deliveryPersonId);
    RoutePlanDTO startActiveDeliveryRoute(Long deliveryPersonId);

    Map<String, PositionDTO> handleWebsocketMessage(MessageDTO messageDTO);
    Map<String, PositionDTO> updateTrackingPosition(Long deliveryPersonId, PositionDTO positionDTO);
    Map<String, PositionDTO> updateTrackingPositionByPackageReference(String packageReference, PositionDTO positionDTO);
    ActiveTrackingPackageDTO getActiveTrackingPackage(Long deliveryPersonId);
    void applySoftLock(Long packageId, String userId);
}
