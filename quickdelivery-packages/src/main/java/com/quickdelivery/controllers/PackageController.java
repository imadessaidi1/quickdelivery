package com.quickdelivery.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.errors.ApiException;
import com.quickdelivery.abstarct.dto.AddressDTO;
import com.quickdelivery.abstarct.dto.ActiveTrackingPackageDTO;
import com.quickdelivery.abstarct.dto.AdminPackageDashboardSummaryDTO;
import com.quickdelivery.abstarct.dto.DeliveryReservationContextDTO;
import com.quickdelivery.abstarct.dto.DocumentContentDTO;
import com.quickdelivery.abstarct.dto.FinancialDashboardDTO;
import com.quickdelivery.abstarct.dto.MessageDTO;
import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.dto.PositionDTO;
import com.quickdelivery.abstarct.dto.ServiceHttpBreakdownDTO;
import com.quickdelivery.abstarct.dto.ServiceLogInsightsDTO;
import com.quickdelivery.abstarct.dto.ServiceMetricsDTO;
import com.quickdelivery.abstarct.entities.Address;
import com.quickdelivery.abstarct.entities.PackageReservation;
import com.quickdelivery.abstarct.helpers.PackegeCSVReader;
import com.quickdelivery.abstarct.parameters.CHECK_STATUS;
import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;
import com.quickdelivery.config.WebSocketHandler;
import com.quickdelivery.dto.ReserveBatchResultDTO;
import com.quickdelivery.observability.RuntimeLogMonitor;
import com.quickdelivery.services.interfaces.IPackagesService;
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
    WebSocketHandler webSocketHandler;
    @Autowired
    RuntimeLogMonitor runtimeLogMonitor;


    @PostMapping("/create")
    public PackageDTO createNewPackage(@RequestParam("packageDTO") String packageDTO,
                                       @RequestParam(value = "files", required = false) MultipartFile[] files,
                                       @RequestParam("locale") Locale locale){
        ObjectMapper objectMapper = new ObjectMapper();
        PackageDTO packageDTO1 = null;
        try {
            packageDTO1 = objectMapper.readValue(packageDTO, PackageDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        PackageDTO aPackage = packagesService.createNewPackage(packageDTO1, files, locale);
        if (PACKAGE_STATUS.NEW.equals(aPackage.getStatus())) {
            notifyPackageCreation(aPackage.getReference());
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
                                                                    @RequestParam(name = "rayonEnMetres", required = true) double rayonEnMetres){
        return packagesService.getPAckagesAroundPosition(latitude,longitude,rayonEnMetres);
    }

    @GetMapping("/packages-around-me")
    public List<PackageDTO> packagesAroundMyPosition(@RequestParam(name = "latitude", required = true) String latitude,
                                                                @RequestParam(name = "longitude", required = true) String longitude,
                                                                @RequestParam(name = "rayonEnMetres", required = true) double rayonEnMetres){
        return packagesService.getPackagesAroundPosition(latitude,longitude,rayonEnMetres);
    }

    @GetMapping("/packages-on-my-road")
    public List<PackageDTO> findPackagesOnMyRoad(@RequestParam(name = "departureLatitude", required = true) String departureLatitude,
                                                   @RequestParam(name = "arrivalLatitude", required = true) String arrivalLatitude,
                                                 @RequestParam(name = "departureLongitude", required = true) String departureLongitude,
                                                 @RequestParam(name = "arrivalLongitude", required = true) String arrivalLongitude){
        return packagesService.findAddressOnMyRoad(departureLatitude,arrivalLatitude,departureLongitude, arrivalLongitude);
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
                notifyPackageCreation(packagesService.findPackageByID(resolvedPackageId).getReference());
            }
        });
    }

    @PutMapping("/confirm-guest-payment")
    public void confirmGuestPayment(@RequestParam("packageID") Long packageID,
                                    @RequestParam("guestAccessToken") String guestAccessToken) {
        packagesService.confirmGuestPackagePayment(packageID, guestAccessToken);
        notifyPackageCreation(packagesService.findPackageByID(packageID).getReference());
    }

    @PutMapping("/reserve")
    public DeliveryReservationContextDTO reservePackage(@RequestParam("packageID") Long packageID,
                                                        @RequestParam("deliveryPersonID") Long deliveryPersonID,
                                                        @RequestParam("locale") Locale locale){
        try {
            PackageReservation packageReservation = packagesService.reservePackage(packageID, deliveryPersonID, locale);
            notifyPackageReservation(packageReservation.getPickUpOTP(), deliveryPersonID, packageReservation.getaPackage().getReference());
            return packagesService.getDeliveryReservationContext(packageID, deliveryPersonID);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/reserve-batch")
    public ReserveBatchResultDTO reservePackagesBatch(@RequestBody List<Long> packageIds,
                                                      @RequestParam("deliveryPersonID") Long deliveryPersonID,
                                                      @RequestParam("locale") Locale locale) {
        return packagesService.reservePackagesBatch(packageIds, deliveryPersonID, locale);
    }

    @PutMapping("/pickup")
    public DeliveryReservationContextDTO pickUpPackage(@RequestParam("packageID") Long packageID,
                                                       @RequestParam("deliveryPersonID") Long deliveryPersonID,
                                                       @RequestParam("pickUpOTP") String pickUpOTP,
                                                       @RequestParam("locale") Locale locale){
        try {
            packagesService.pickUpPackage(packageID,deliveryPersonID,pickUpOTP,locale);
            notifyPackagePickup(packageID);
            return packagesService.getDeliveryReservationContext(packageID, deliveryPersonID);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/deliver")
    public void deliverPackage(@RequestParam("packageID") Long packageID,
                              @RequestParam("deliveryPersonID") Long deliveryPersonID,
                              @RequestParam("deliveryOTP") String deliveryOTP,
                              @RequestParam("locale") Locale locale){
        try {
            packagesService.deliverPackage(packageID,deliveryPersonID,deliveryOTP,locale);
            notifyPackageDelivery(packageID);
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
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setFrom("PACKAGE_SERVICE");
        messageDTO.setType("NEW_PACKAGE_NOTIFICATION");
        messageDTO.setTo("1652");
        messageDTO.setMessage("There is a new package around you :)");
        ObjectMapper objectMapper = new ObjectMapper();
        String json;
        try {
            json = objectMapper.writeValueAsString(messageDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        webSocketHandler.sendMessageToAll(json);
    }

    @GetMapping("/isUserWithOngoingDelivery")
    public Boolean isUserWithOngoingDelivery(@RequestParam("userId") Long userId){
        return packagesService.isUserWithOngoingDelivery(userId);
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
                                                          @RequestParam(name = "line1", required = true) String line1,
                                                          @RequestParam(name = "zipCode", required = true) String zipCode,
                                                          @RequestParam(name = "town", required = true) String town,
                                                          @RequestParam(name = "country", required = true) String country,
                                                          @RequestParam(name = "rayonEnMetres", required = true) double rayonEnMetres) {
        AddressDTO addressDTO = new AddressDTO();
        addressDTO.setLine1(line1);
        addressDTO.setCountry(country);
        addressDTO.setTown(town);
        addressDTO.setZipCode(zipCode);
        try {
            return packagesService.getPackagesAroundPositionWithDestination(latitude, longitude, addressDTO, rayonEnMetres);
        } catch (IOException | InterruptedException | ApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void notifyPackageCreation(String aPackage){
        List<Address> addressAroundNewPackage = packagesService.findUsersAroundPosition(aPackage);
        for (Address address : addressAroundNewPackage){
            MessageDTO messageDTO = new MessageDTO();
            messageDTO.setFrom("PACKAGE_SERVICE");
            messageDTO.setType("NEW_PACKAGE_NOTIFICATION");
            messageDTO.setTo(address.getResidents().getId().toString());
            messageDTO.setMessage("There is a new package around you :)");
            messageDTO.setUrl(PACKAGE_CONSULTATION_PATH + aPackage);
            ObjectMapper objectMapper = new ObjectMapper();
            String json;
            try {
                json = objectMapper.writeValueAsString(messageDTO);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            webSocketHandler.sendMessageToAll(json);
        };
    }

    private void notifyPackageReservation(String otp, Long userID, String packageReference){
            MessageDTO messageDTO = new MessageDTO();
            messageDTO.setFrom("PACKAGE_SERVICE");
            messageDTO.setType("PACKAGE_RESERVATION_OTP_NOTIFICATION");
            messageDTO.setTo(userID.toString());
            messageDTO.setMessage("You reserved a package. Here is the password to pick it up : "+otp);
            messageDTO.setUrl(PACKAGE_CONSULTATION_PATH + packageReference);
            ObjectMapper objectMapper = new ObjectMapper();
            String json;
            try {
                json = objectMapper.writeValueAsString(messageDTO);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            webSocketHandler.sendMessageToAll(json);
    }

    private void notifyPackagePickup(Long packageID) {
        PackageDTO packageDTO = packagesService.findPackageByID(packageID);
        if (packageDTO.getSenderID() == null) {
            return;
        }
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setFrom("PACKAGE_SERVICE");
        messageDTO.setType("PACKAGE_PICKUP_NOTIFICATION");
        messageDTO.setTo(packageDTO.getSenderID().toString());
        messageDTO.setMessage("Your package has been picked up.");
        messageDTO.setUrl(PACKAGE_CONSULTATION_PATH + packageDTO.getReference());
        sendSocketMessage(messageDTO);
    }

    private void notifyPackageDelivery(Long packageID) {
        PackageDTO packageDTO = packagesService.findPackageByID(packageID);
        if (packageDTO.getSenderID() == null) {
            return;
        }
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setFrom("PACKAGE_SERVICE");
        messageDTO.setType("PACKAGE_DELIVERY_NOTIFICATION");
        messageDTO.setTo(packageDTO.getSenderID().toString());
        messageDTO.setMessage("Your package has been delivered.");
        messageDTO.setUrl(PACKAGE_CONSULTATION_PATH + packageDTO.getReference());
        sendSocketMessage(messageDTO);
    }

    private void sendSocketMessage(MessageDTO messageDTO) {
        ObjectMapper objectMapper = new ObjectMapper();
        String json;
        try {
            json = objectMapper.writeValueAsString(messageDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        webSocketHandler.sendMessageToAll(json);
    }
}
