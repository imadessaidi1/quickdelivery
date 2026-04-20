package com.quickdelivery.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.errors.ApiException;
import com.quickdelivery.abstarct.dto.AddressDTO;
import com.quickdelivery.abstarct.dto.ActiveTrackingPackageDTO;
import com.quickdelivery.abstarct.dto.AdminPackageDashboardSummaryDTO;
import com.quickdelivery.abstarct.dto.DeliveryReservationContextDTO;
import com.quickdelivery.abstarct.dto.DocumentContentDTO;
import com.quickdelivery.abstarct.dto.CourierPenaltyDTO;
import com.quickdelivery.abstarct.dto.FinancialDashboardDTO;
import com.quickdelivery.abstarct.dto.MobileDeviceRegistrationDTO;
import com.quickdelivery.abstarct.dto.NotificationDTO;
import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.dto.PositionDTO;
import com.quickdelivery.abstarct.dto.ServiceHttpBreakdownDTO;
import com.quickdelivery.abstarct.dto.ServiceLogInsightsDTO;
import com.quickdelivery.abstarct.dto.ServiceMetricsDTO;
import com.quickdelivery.abstarct.entities.Address;
import com.quickdelivery.abstarct.entities.PackageReservation;
import com.quickdelivery.abstarct.helpers.PackegeCSVReader;
import com.quickdelivery.abstarct.parameters.CHECK_STATUS;
import com.quickdelivery.abstarct.parameters.NOTIFICATION_EVENT_TYPE;
import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;
import com.quickdelivery.abstarct.security.CaptchaVerificationService;
import com.quickdelivery.dto.ReserveBatchPlanRequestDTO;
import com.quickdelivery.dto.ReserveBatchResultDTO;
import com.quickdelivery.dto.ReservationAvailabilityDTO;
import com.quickdelivery.dto.RoutePlanDTO;
import com.quickdelivery.dto.RoutePlanRequestDTO;
import com.quickdelivery.observability.RuntimeLogMonitor;
import com.quickdelivery.services.interfaces.INotificationService;
import com.quickdelivery.services.interfaces.IPackagesService;
import jakarta.servlet.http.HttpServletRequest;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@EnableAspectJAutoProxy
@RequestMapping("/packages/v1")
public class PackageController {
    private static final String PACKAGE_CONSULTATION_PATH = "/package?id=";
    @Autowired
    private IPackagesService packagesService;
    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    RuntimeLogMonitor runtimeLogMonitor;
    @Autowired
    private CaptchaVerificationService captchaVerificationService;
    @Autowired
    private INotificationService notificationService;


    @PostMapping("/create")
    public PackageDTO createNewPackage(@RequestParam("packageDTO") String packageDTO,
                                       @RequestParam(value = "files", required = false) MultipartFile[] files,
                                       @RequestParam("locale") Locale locale,
                                       @RequestHeader(value = "X-Captcha-Token", required = false) String captchaToken,
                                       HttpServletRequest servletRequest){
        ObjectMapper objectMapper = new ObjectMapper();
        PackageDTO packageDTO1 = null;
        try {
            packageDTO1 = objectMapper.readValue(packageDTO, PackageDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        if (packageDTO1.getSenderID() == null || Boolean.TRUE.equals(packageDTO1.getGuestMode())) {
            captchaVerificationService.validateOrThrow(
                    captchaToken,
                    captchaVerificationService.resolveClientIp(servletRequest.getHeader("X-Forwarded-For"), servletRequest.getRemoteAddr()),
                    "packages-create"
            );
        }
        PackageDTO aPackage = packagesService.createNewPackage(packageDTO1, files, locale);
        if (PACKAGE_STATUS.NEW.equals(aPackage.getStatus())) {
            notifyPackageCreation(aPackage.getReference());
            notifyPaymentConfirmedForSender(aPackage);
        } else {
            notifyPackageCreatedForSender(aPackage);
        }
        return  aPackage;
    }

    @PostMapping("/bulk-create")
    public void createNewPackages(@RequestBody List<PackageDTO> packageDTOS, Locale locale){
        packagesService.createNewPackages(packageDTOS, locale);
    }

    @PostMapping("/estimate-price")
    public PackageDTO estimateDeliveryPrice(@RequestBody PackageDTO packageDTO) {
        return packagesService.estimateDeliveryPrice(packageDTO);
    }

    @GetMapping("/packages-around")
    //@Cacheable(value="PackagesAroundMe", keyGenerator="customKeyGenerator")
    public Map<String, List<PackageDTO>> packagesAroundPosition(@RequestParam(name = "latitude", required = true) String latitude,
                                                                    @RequestParam(name = "longitude", required = true) String longitude,
                                                                    @RequestParam(name = "rayonEnMetres", required = true) double rayonEnMetres,
                                                                    @RequestParam(name = "deliveryMode", required = false) String deliveryMode){
        return packagesService.getPAckagesAroundPosition(latitude,longitude,rayonEnMetres, deliveryMode);
    }

    @GetMapping("/packages-around-me")
    public List<PackageDTO> packagesAroundMyPosition(@RequestParam(name = "latitude", required = true) String latitude,
                                                                @RequestParam(name = "longitude", required = true) String longitude,
                                                                @RequestParam(name = "rayonEnMetres", required = true) double rayonEnMetres,
                                                                @RequestParam(name = "deliveryMode", required = false) String deliveryMode,
                                                                @RequestParam(name = "deliveryPersonId", required = false) Long deliveryPersonId){
        return packagesService.getPackagesAroundPosition(latitude,longitude,rayonEnMetres, deliveryMode, deliveryPersonId);
    }

    @GetMapping("/map/packages-in-bounds")
    public List<PackageDTO> packagesInBounds(@RequestParam(name = "minLat") double minLat,
                                             @RequestParam(name = "maxLat") double maxLat,
                                             @RequestParam(name = "minLng") double minLng,
                                             @RequestParam(name = "maxLng") double maxLng,
                                             @RequestParam(name = "centerLat") double centerLat,
                                             @RequestParam(name = "centerLng") double centerLng,
                                             @RequestParam(name = "limit", defaultValue = "100") int limit,
                                             @RequestParam(name = "deliveryMode", required = false) String deliveryMode,
                                             @RequestParam(name = "deliveryPersonId", required = false) Long deliveryPersonId) {
        return packagesService.getPackagesInBounds(minLat, maxLat, minLng, maxLng, centerLat, centerLng, limit, deliveryMode, deliveryPersonId);
    }

    @GetMapping("/packages-on-my-road")
    public List<PackageDTO> findPackagesOnMyRoad(@RequestParam(name = "departureLatitude", required = true) String departureLatitude,
                                                   @RequestParam(name = "arrivalLatitude", required = true) String arrivalLatitude,
                                                 @RequestParam(name = "departureLongitude", required = true) String departureLongitude,
                                                 @RequestParam(name = "arrivalLongitude", required = true) String arrivalLongitude,
                                                 @RequestParam(name = "deliveryMode", required = false) String deliveryMode,
                                                 @RequestParam(name = "vehicleType", required = false) String vehicleType,
                                                 @RequestParam(name = "radiusMeters", required = false, defaultValue = "10000") double radiusMeters,
                                                 @RequestParam(name = "deliveryPersonId", required = false) Long deliveryPersonId){
        return packagesService.findAddressOnMyRoad(departureLatitude,arrivalLatitude,departureLongitude, arrivalLongitude, deliveryMode, vehicleType, radiusMeters, deliveryPersonId);
    }

    @PostMapping("/plan-route")
    public RoutePlanDTO planRoute(@RequestBody RoutePlanRequestDTO request) {
        return packagesService.buildRoutePlan(request);
    }

    @GetMapping("/package-by-status")
    public List<PackageDTO> findPackagesByStatus(@RequestParam(name = "status", required = true) PACKAGE_STATUS status){
        return packagesService.findPackagesByStatus(status);
    }

    @PostMapping("/upload")
    public void createNewPackagesFromCSV(@RequestParam("file") MultipartFile file, Locale locale){
        List<PackageDTO> packageDTOList = PackegeCSVReader.CSVToPackages(file);
        packagesService.createNewPackages(packageDTOList, locale);
    }

    @PutMapping("/update-packages-status")
    public void updatePackageStatus(@RequestParam Map<String, String> requestMap){
        requestMap.forEach((packageId, packageStatus) -> {
            PACKAGE_STATUS targetStatus = PACKAGE_STATUS.valueOf(packageStatus);
            Long resolvedPackageId = Long.valueOf(packageId);
            packagesService.updatePackageStatus(targetStatus, resolvedPackageId);
            if (PACKAGE_STATUS.NEW.equals(targetStatus)) {
                PackageDTO aPackage = packagesService.findPackageByID(resolvedPackageId);
                notifyPackageCreation(aPackage.getReference());
                notifyPaymentConfirmedForSender(aPackage);
            }
        });
    }

    @PutMapping("/confirm-guest-payment")
    public void confirmGuestPayment(@RequestParam("packageID") Long packageID,
                                    @RequestParam("guestAccessToken") String guestAccessToken) {
        packagesService.confirmGuestPackagePayment(packageID, guestAccessToken);
        PackageDTO aPackage = packagesService.findPackageByID(packageID);
        notifyPackageCreation(aPackage.getReference());
        notifyPaymentConfirmedForSender(aPackage);
    }

    @PutMapping("/reserve")
    public DeliveryReservationContextDTO reservePackage(@RequestParam("packageID") Long packageID,
                                                        @RequestParam("deliveryPersonID") Long deliveryPersonID,
                                                        @RequestParam("locale") Locale locale){
        try {
            packagesService.reservePackage(packageID, deliveryPersonID, locale);
            return packagesService.getDeliveryReservationContext(packageID, deliveryPersonID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/reserve-batch")
    public ReserveBatchResultDTO reservePackagesBatch(@RequestBody List<Long> packageIds,
                                                      @RequestParam("deliveryPersonID") Long deliveryPersonID,
                                                      @RequestParam("locale") Locale locale) {
        return packagesService.reservePackagesBatch(packageIds, deliveryPersonID, locale);
    }

    @PutMapping("/reserve-batch-planned")
    public ReserveBatchResultDTO reservePackagesBatchPlanned(@RequestBody ReserveBatchPlanRequestDTO request,
                                                             @RequestParam("deliveryPersonID") Long deliveryPersonID,
                                                             @RequestParam("locale") Locale locale) {
        return packagesService.reservePackagesBatchFromPlan(request, deliveryPersonID, locale);
    }

    @PutMapping("/cancel-reservation")
    public PackageDTO cancelReservation(@RequestParam("packageID") Long packageID,
                                        @RequestParam("deliveryPersonID") Long deliveryPersonID) {
        return packagesService.cancelReservation(packageID, deliveryPersonID);
    }

    @PutMapping("/pickup")
    public DeliveryReservationContextDTO pickUpPackage(@RequestParam("packageID") Long packageID,
                                                       @RequestParam("deliveryPersonID") Long deliveryPersonID,
                                                       @RequestParam("pickUpOTP") String pickUpOTP,
                                                       @RequestParam("currentLatitude") Double currentLatitude,
                                                       @RequestParam("currentLongitude") Double currentLongitude,
                                                       @RequestParam("locale") Locale locale){
        try {
            PositionDTO currentPosition = new PositionDTO();
            currentPosition.setLatitude(BigDecimal.valueOf(currentLatitude));
            currentPosition.setLongitude(BigDecimal.valueOf(currentLongitude));
            packagesService.pickUpPackage(packageID,deliveryPersonID,pickUpOTP,locale, currentPosition);
            return packagesService.getDeliveryReservationContext(packageID, deliveryPersonID);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/deliver")
    public void deliverPackage(@RequestParam("packageID") Long packageID,
                              @RequestParam("deliveryPersonID") Long deliveryPersonID,
                              @RequestParam("deliveryOTP") String deliveryOTP,
                              @RequestParam("currentLatitude") Double currentLatitude,
                              @RequestParam("currentLongitude") Double currentLongitude,
                              @RequestParam("locale") Locale locale){
        try {
            PositionDTO currentPosition = new PositionDTO();
            currentPosition.setLatitude(BigDecimal.valueOf(currentLatitude));
            currentPosition.setLongitude(BigDecimal.valueOf(currentLongitude));
            packagesService.deliverPackage(packageID,deliveryPersonID,deliveryOTP,locale, currentPosition);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/checkOTPForPickup")
    public CHECK_STATUS checkOTPForPickUpPackage(@RequestParam("packageID") Long packageID,
                                                 @RequestParam("senderID") Long senderID,
                                                 @RequestParam("pickUpOTP") String pickUpOTP){
            return packagesService.checkOTPForPickUpPackage(packageID,senderID,pickUpOTP);
    }

    @GetMapping("/checkOTPForDelivery")
    public CHECK_STATUS checkOTPForDeliveryPackage(@RequestParam("packageID") Long packageID,
                                           @RequestParam("deliveryPersonID") Long deliveryPersonID,
                                           @RequestParam("deliveryOTP") String deliveryOTP){
        return packagesService.checkOTPForDeliverPackage(packageID,deliveryPersonID,deliveryOTP);
    }

    @GetMapping("/checkGuestOTPForPickup")
    public CHECK_STATUS checkGuestOTPForPickUpPackage(@RequestParam("packageID") Long packageID,
                                                      @RequestParam("guestAccessToken") String guestAccessToken,
                                                      @RequestParam("pickUpOTP") String pickUpOTP){
        return packagesService.checkGuestOTPForPickUpPackage(packageID, guestAccessToken, pickUpOTP);
    }

    @GetMapping("/checkGuestOTPForDelivery")
    public CHECK_STATUS checkGuestOTPForDeliveryPackage(@RequestParam("packageID") Long packageID,
                                                        @RequestParam("guestAccessToken") String guestAccessToken,
                                                        @RequestParam("deliveryOTP") String deliveryOTP){
        return packagesService.checkGuestOTPForDeliverPackage(packageID, guestAccessToken, deliveryOTP);
    }
    @GetMapping("/getPackagesByDeliveryPerson")
    public Map<PACKAGE_STATUS, List<PackageDTO>> getPackagesByDeliveryPerson(@RequestParam("deliveryPersonID") Long deliveryPersonID){
        return packagesService.getPackagesByDeliveryPerson(deliveryPersonID);
    }

    @GetMapping("/getPackagesBySender")
    public Map<PACKAGE_STATUS, List<PackageDTO>> getPackagesBySender(@RequestParam("senderID") Long senderID){
        return packagesService.getPackagesBySender(senderID);
    }

    @GetMapping("/delivery-context")
    public DeliveryReservationContextDTO getDeliveryReservationContext(@RequestParam("packageID") Long packageID,
                                                                       @RequestParam("deliveryPersonID") Long deliveryPersonID) {
        return packagesService.getDeliveryReservationContext(packageID, deliveryPersonID);
    }

    @GetMapping("/getPackage")
    public PackageDTO getPackagesByID(@RequestParam("reference") String reference){
        return packagesService.findPackageByReference(reference);
    }

    @GetMapping("/getGuestPackage")
    public PackageDTO getGuestPackage(@RequestParam("reference") String reference,
                                      @RequestParam("guestAccessToken") String guestAccessToken){
        return packagesService.findGuestPackageByReference(reference, guestAccessToken);
    }

    @PostMapping("/tracking/position")
    public void updateTrackingPosition(@RequestParam("deliveryPersonId") Long deliveryPersonId,
                                       @RequestBody PositionDTO positionDTO) {
        packagesService.updateTrackingPosition(deliveryPersonId, positionDTO);
    }

    @PostMapping("/tracking/package-position")
    public void updateTrackingPositionByPackageReference(@RequestParam("packageReference") String packageReference,
                                                         @RequestBody PositionDTO positionDTO) {
        packagesService.updateTrackingPositionByPackageReference(packageReference, positionDTO);
    }

    @GetMapping("/tracking/active-package")
    public ActiveTrackingPackageDTO getActiveTrackingPackage(@RequestParam("deliveryPersonId") Long deliveryPersonId) {
        return packagesService.getActiveTrackingPackage(deliveryPersonId);
    }

    @GetMapping("/document-content")
    public ResponseEntity<byte[]> loadDocumentContent(@RequestParam("documentId") Long documentId) {
        DocumentContentDTO documentContent = packagesService.loadPackageDocumentContent(documentId);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (documentContent.getContentType() != null && !documentContent.getContentType().isBlank()) {
            mediaType = MediaType.parseMediaType(documentContent.getContentType());
        }
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok().contentType(mediaType);
        if (documentContent.getFileName() != null && !documentContent.getFileName().isBlank()) {
            responseBuilder.header(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + documentContent.getFileName() + "\"");
        }
        return responseBuilder.body(documentContent.getData());
    }

    @GetMapping("/notify")
    public void notifyClient(){
        notificationService.createAndDispatch(
                1652L,
                NOTIFICATION_EVENT_TYPE.PACKAGE_NEARBY,
                "",
                "{}"
        );
    }

    @GetMapping("/isUserWithOngoingDelivery")
    public Boolean isUserWithOngoingDelivery(@RequestParam("userId") Long userId){
        return packagesService.isUserWithOngoingDelivery(userId);
    }

    @GetMapping("/active-route")
    public RoutePlanDTO activeDeliveryRoute(@RequestParam("deliveryPersonId") Long deliveryPersonId) {
        return packagesService.getActiveDeliveryRoute(deliveryPersonId);
    }

    @PutMapping("/start-active-route")
    public RoutePlanDTO startActiveRoute(@RequestParam("deliveryPersonId") Long deliveryPersonId) {
        return packagesService.startActiveDeliveryRoute(deliveryPersonId);
    }

    @PutMapping("/cancel-active-route")
    public RoutePlanDTO cancelActiveRoute(@RequestParam("deliveryPersonId") Long deliveryPersonId) {
        return packagesService.cancelActiveDeliveryRoute(deliveryPersonId);
    }

    @GetMapping("/reservation-availability")
    public ReservationAvailabilityDTO reservationAvailability(@RequestParam("deliveryPersonId") Long deliveryPersonId) {
        return packagesService.getReservationAvailability(deliveryPersonId);
    }

    @GetMapping("/admin/metrics")
    public ServiceMetricsDTO adminMetrics() {
        return packagesService.loadAdminMetrics();
    }

    @GetMapping("/admin/tracking-metrics")
    public ServiceMetricsDTO adminTrackingMetrics() {
        return packagesService.loadAdminTrackingMetrics();
    }

    @GetMapping("/admin/http-breakdown")
    public ServiceHttpBreakdownDTO adminHttpBreakdown() {
        return packagesService.loadAdminHttpBreakdown();
    }

    @GetMapping("/admin/dashboard-summary")
    public AdminPackageDashboardSummaryDTO adminDashboardSummary(@RequestParam(name = "year", required = false) Integer year) {
        int resolvedYear = year == null ? java.time.LocalDate.now().getYear() : year;
        return packagesService.loadAdminDashboardSummary(resolvedYear);
    }

    @GetMapping("/admin/log-insights")
    public ServiceLogInsightsDTO adminLogInsights() {
        return runtimeLogMonitor.snapshot("packages-service");
    }

    @GetMapping("/admin/financial-dashboard")
    public FinancialDashboardDTO adminFinancialDashboard() {
        return packagesService.loadAdminFinancialDashboard();
    }

    @GetMapping("/admin/courier-penalties")
    public List<CourierPenaltyDTO> adminCourierPenalties(@RequestParam(name = "limit", required = false, defaultValue = "100") int limit) {
        return packagesService.loadAdminCourierPenalties(limit);
    }

    @PostMapping("/admin/courier-penalties/{penaltyId}/lift")
    public CourierPenaltyDTO liftCourierPenalty(@PathVariable("penaltyId") Long penaltyId,
                                                @RequestParam(name = "reason", required = false) String reason) {
        return packagesService.liftCourierPenalty(penaltyId, reason);
    }

    @GetMapping("/notifications")
    public List<NotificationDTO> notifications(@RequestParam("userId") Long userId) {
        return notificationService.findByRecipient(userId);
    }

    @PostMapping("/notifications/mark-read")
    public void markNotificationRead(@RequestParam("notificationId") Long notificationId,
                                     @RequestParam("userId") Long userId) {
        notificationService.markAsRead(notificationId, userId);
    }

    @PostMapping("/notifications/mark-all-read")
    public void markAllNotificationsRead(@RequestParam("userId") Long userId) {
        notificationService.markAllAsRead(userId);
    }

    @PostMapping("/devices/register")
    public MobileDeviceRegistrationDTO registerMobileDevice(@RequestBody MobileDeviceRegistrationDTO request) {
        return notificationService.registerMobileDevice(request);
    }

    @PostMapping("/notifications/preferences")
    public void updateNotificationPreferences(@RequestParam("userId") Long userId,
                                              @RequestParam("locale") String localeCode) {
        notificationService.updateUserPreferredLocale(userId, localeCode);
    }

    @DeleteMapping("/devices/unregister")
    public void unregisterMobileDevice(@RequestParam("userId") Long userId,
                                       @RequestParam("deviceId") String deviceId) {
        notificationService.unregisterMobileDevice(userId, deviceId);
    }

    @GetMapping("/packages-around-address")
    public Map<String, List<PackageDTO>> packagesAroundAddress(@RequestParam(name = "line1", required = true) String line1,
                                                               @RequestParam(name = "zipCode", required = true) String zipCode,
                                                               @RequestParam(name = "town", required = true) String town,
                                                               @RequestParam(name = "country", required = true) String country,
                                                               @RequestParam(name = "rayonEnMetres", required = true) double rayonEnMetres){
        AddressDTO addressDTO = new AddressDTO();
        addressDTO.setLine1(line1);
        addressDTO.setCountry(country);
        addressDTO.setTown(town);
        addressDTO.setZipCode(zipCode);
        try {
            return packagesService.getPAckagesAroundPosition(addressDTO,rayonEnMetres);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/packages-around-me-by-destination")
    public List<PackageDTO> packagesAroundMeByDestination(@RequestParam(name = "latitude", required = true) String latitude,
                                                          @RequestParam(name = "longitude", required = true) String longitude,
                                                          @RequestParam(name = "line1", required = false, defaultValue = "") String line1,
                                                          @RequestParam(name = "zipCode", required = false, defaultValue = "") String zipCode,
                                                          @RequestParam(name = "town", required = false, defaultValue = "") String town,
                                                          @RequestParam(name = "country", required = false, defaultValue = "") String country,
                                                          @RequestParam(name = "rayonEnMetres", required = true) double rayonEnMetres,
                                                          @RequestParam(name = "pickupRadiusMeters", required = false) Double pickupRadiusMeters,
                                                          @RequestParam(name = "deliveryRadiusMeters", required = false) Double deliveryRadiusMeters,
                                                          @RequestParam(name = "deliveryMode", required = false) String deliveryMode,
                                                          @RequestParam(name = "vehicleType", required = false) String vehicleType,
                                                          @RequestParam(name = "destinationLatitude", required = false) Double destinationLatitude,
                                                          @RequestParam(name = "destinationLongitude", required = false) Double destinationLongitude,
                                                          @RequestParam(name = "deliveryPersonId", required = false) Long deliveryPersonId) {
        AddressDTO addressDTO = new AddressDTO();
        addressDTO.setLine1(line1);
        addressDTO.setCountry(country);
        addressDTO.setTown(town);
        addressDTO.setZipCode(zipCode);
        addressDTO.setLatitude(destinationLatitude == null ? null : BigDecimal.valueOf(destinationLatitude));
        addressDTO.setLongitude(destinationLongitude == null ? null : BigDecimal.valueOf(destinationLongitude));
        double effectivePickupRadius = pickupRadiusMeters != null ? pickupRadiusMeters : rayonEnMetres;
        double effectiveDeliveryRadius = deliveryRadiusMeters != null ? deliveryRadiusMeters : rayonEnMetres;
        try {
            return packagesService.getPackagesAroundPositionWithDestination(latitude, longitude, addressDTO, effectivePickupRadius, effectiveDeliveryRadius, deliveryMode, vehicleType, deliveryPersonId);
        } catch (IOException | InterruptedException | ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void notifyPackageCreation(String aPackage){
        PackageDTO packageDTO = packagesService.findPackageByReference(aPackage);
        AddressDTO departureAddress = packageDTO == null ? null : packageDTO.getAddresses().stream()
                .filter(address -> address.getType() != null && "DEPARTURE".equals(address.getType().name()))
                .findFirst()
                .orElse(null);

        List<Long> nearbyRecipientIds = departureAddress == null
                || departureAddress.getLatitude() == null
                || departureAddress.getLongitude() == null
                ? List.of()
                : notificationService.findNearbyCourierRecipientIds(
                        departureAddress.getLatitude(),
                        departureAddress.getLongitude(),
                        20000d
                );

        if (!nearbyRecipientIds.isEmpty()) {
            for (Long recipientId : nearbyRecipientIds) {
                notificationService.createAndDispatch(
                        recipientId,
                        NOTIFICATION_EVENT_TYPE.PACKAGE_NEARBY,
                        PACKAGE_CONSULTATION_PATH + aPackage,
                        "{\"packageReference\":\"" + aPackage + "\"}"
                );
            }
            return;
        }

        List<Address> addressAroundNewPackage = packagesService.findUsersAroundPosition(aPackage);
        for (Address address : addressAroundNewPackage){
            notificationService.createAndDispatch(
                    address.getResidents().getId(),
                    NOTIFICATION_EVENT_TYPE.PACKAGE_NEARBY,
                    PACKAGE_CONSULTATION_PATH + aPackage,
                    "{\"packageReference\":\"" + aPackage + "\"}"
            );
        }
    }

    private void notifyPackageCreatedForSender(PackageDTO packageDTO) {
        if (packageDTO == null || packageDTO.getSenderID() == null || packageDTO.getReference() == null) {
            return;
        }
        notificationService.createAndDispatch(
                packageDTO.getSenderID(),
                NOTIFICATION_EVENT_TYPE.PACKAGE_CREATED,
                PACKAGE_CONSULTATION_PATH + packageDTO.getReference(),
                "{\"packageReference\":\"" + packageDTO.getReference() + "\"}"
        );
    }

    private void notifyPaymentConfirmedForSender(PackageDTO packageDTO) {
        if (packageDTO == null || packageDTO.getSenderID() == null || packageDTO.getReference() == null) {
            return;
        }
        notificationService.createAndDispatch(
                packageDTO.getSenderID(),
                NOTIFICATION_EVENT_TYPE.PACKAGE_PAYMENT_CONFIRMED,
                PACKAGE_CONSULTATION_PATH + packageDTO.getReference(),
                "{\"packageReference\":\"" + packageDTO.getReference() + "\"}",
                packageDTO.getReference()
        );
    }

}
