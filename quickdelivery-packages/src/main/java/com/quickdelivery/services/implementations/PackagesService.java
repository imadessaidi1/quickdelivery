package com.quickdelivery.services.implementations;

import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DistanceMatrix;
import com.quickdelivery.PublicFrontendUrlResolver;
import com.quickdelivery.abstarct.dto.*;
import com.quickdelivery.abstarct.entities.*;
import com.quickdelivery.abstarct.entities.Package;
import com.quickdelivery.abstarct.helpers.*;
import com.quickdelivery.abstarct.parameters.*;
import com.quickdelivery.abstarct.repositories.CourierPayouts;
import com.quickdelivery.abstarct.repositories.Documents;
import com.quickdelivery.abstarct.repositories.PackageSettlements;
import com.quickdelivery.abstarct.repositories.Packages;
import com.quickdelivery.abstarct.repositories.Users;
import com.quickdelivery.dto.ReserveBatchResultDTO;
import com.quickdelivery.helpers.PackageDeliveryPriceCalculator;
import com.quickdelivery.helpers.PackagePricingBreakdown;
import com.quickdelivery.services.interfaces.IPackagesService;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.Timer;
import jakarta.mail.MessagingException;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class PackagesService implements IPackagesService {
    @Value("${application.otp.secret}")
    private String OTPSecret;
    @Value("${application.otp.counter}")
    private long OTPCounter;
    @Value("${mapquest.geocode.url.part1}")
    private String mapQuestURL1;
    @Value("${mapquest.geocode.url.part2}")
    private String mapQuestURL2;
    @Value("${mapquest.key}")
    private String mapQuestKey;
    @Value("${package.services.getbyid.url}")
    private String getPackageByIdURL;
    @Value("${package.services.qrcode.file.location}")
    private String qrCodePath;
    @Value("${packages.docs.directory}")
    private String packagesDirectory;
    @Value("${package.doc.labelends}")
    private String packageLabelEnds;
    @Value("${package.doc.qrends}")
    private String packageQrEnds;
    @Value("${package.referece.start}")
    private String packageReferenceStart;
    @Value("${package.services.followup.url}")
    private String packageFollowupLink;
    @Value("${package.services.evaluate.url}")
    private String evaluateLink;
    @Value("${quickdelivery.frontend.base-url:}")
    private String frontendBaseUrl;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private Packages packages;
    @Autowired
    private Documents documents;
    @Autowired
    private CourierPayouts courierPayouts;
    @Autowired
    private PackageSettlements packageSettlements;
    @Autowired
    private Users users;
    @Autowired
    private GeoApiContext geoApiContext;
    @Autowired
    private Logger logger;
    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    private ResourceBundleMessageSource messageSource;
    @Autowired()
    @Qualifier("myTemplateResolver")
    private ITemplateResolver templateResolver;
    @Autowired
    @Qualifier("packageAsyncTaskExecutor")
    private Executor packageAsyncTaskExecutor;
    @Autowired
    @Qualifier("packageMailTaskExecutor")
    private Executor packageMailTaskExecutor;
    private static final int FINANCIAL_TREND_MONTHS = 6;
    private static final int FINANCIAL_RECENT_SETTLEMENT_LIMIT = 8;
    private static final int FINANCIAL_PENDING_PAYOUT_LIMIT = 8;
    private static final Set<COURIER_PAYOUT_STATUS> OPEN_PAYOUT_STATUSES = EnumSet.of(
            COURIER_PAYOUT_STATUS.PENDING,
            COURIER_PAYOUT_STATUS.APPROVED,
            COURIER_PAYOUT_STATUS.FAILED
    );
    @Override
    public PackageDTO createNewPackage(PackageDTO packageDTO, MultipartFile[] files, Locale locale) {
        return recordPackageOperation("create", () -> {
            MultiValueMap<String, MultipartFile> filesMap = new LinkedMultiValueMap<>();
            Package aPackage;
            try {
                validatePackageCreationRequest(packageDTO);
                packageAddressGeocoding(packageDTO);
                DistanceMatrix distanceMatrix = packageDistanceCalculation(packageDTO);
                PackagePricingBreakdown pricingBreakdown = calculatePricingBreakdown(packageDTO, distanceMatrix);
                aPackage = preparPackage(packageDTO, locale, pricingBreakdown);
                if (files != null && files.length > 0) {
                    IntStream.range(0, files.length)
                            .forEach(index -> {
                                MultipartFile file = files[index];
                                DOCUMENT_TYPE docType = index == 0 ? DOCUMENT_TYPE.PACKAGE_PICTURE : DOCUMENT_TYPE.PACKAGE_INVOICE;
                                String fileName = docType.toString();
                                String currentFileName = file.getOriginalFilename();
                                String newFileName = fileName + currentFileName.substring(currentFileName.lastIndexOf('.'));
                                Document document = new Document();
                                document.setaPackage(aPackage);
                                document.setDocURL(packagesDirectory + aPackage.getReference() + "\\" + newFileName);
                                document.setType(docType);
                                filesMap.add(fileName, file);
                                aPackage.getDocument().add(document);
                            });
                }
                packages.saveAndFlush(aPackage);
                packageDTO.setId(aPackage.getId());
                packageDTO.setVersion(aPackage.getVersion());
                packageDTO.setGuestMode(Boolean.TRUE.equals(aPackage.getGuestMode()));
                packageDTO.setGuestAccessToken(aPackage.getGuestAccessToken());
                String packageDirectory = packagesDirectory + aPackage.getReference();
                try {
                    if (!filesMap.isEmpty()) {
                        FileHelper.saveFilesInParallel(filesMap, packageDirectory, false);
                    }
                } catch (Exception fileFailure) {
                    rollbackFailedPackageCreation(aPackage, packageDirectory);
                    throw fileFailure;
                }
                CompletableFuture.runAsync(() -> {
                    try {
                        generatePackageArtifacts(packageDTO, locale);
                        sendPackageCreationEMail(
                                aPackage,
                                locale,
                                EMAIL_TEMPLATE_TYPE.PACKAGE_CREATION.getType(),
                                messageSource.getMessage("email.subject.newPackage", null, locale),
                                EMAIL_TYPE.PACKAGE_CREATION
                        );
                    } catch (Exception e) {
                        logger.error("Unable to generate package artifacts and send creation email for {}", packageDTO.getReference(), e);
                    }
                }, packageAsyncTaskExecutor);
                return packageDTO;
            } catch (MalformedURLException | FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public PackageDTO estimateDeliveryPrice(PackageDTO packageDTO) {
        return recordPackageOperation("estimatePrice", () -> {
            validatePackageCreationRequest(packageDTO);
            packageAddressGeocoding(packageDTO);
            DistanceMatrix distanceMatrix = packageDistanceCalculation(packageDTO);
            applyPricing(packageDTO, calculatePricingBreakdown(packageDTO, distanceMatrix));
            return packageDTO;
        });
    }

    private void generatePackageArtifacts(PackageDTO packageDTO, Locale locale) throws IOException {
        String packageBasePath = packagesDirectory + packageDTO.getReference();
        String qrPath = packageBasePath + packageQrEnds;
        String labelPath = packageBasePath + packageLabelEnds;
        String tempLabelPath = labelPath + ".tmp";
        QRCodeGenerator.generateQRCode(buildPackageConsultationLink(packageDTO.getReference()), qrPath, 150, 150);
        Path qrFile = Paths.get(qrPath);
        if (!Files.exists(qrFile) || Files.size(qrFile) == 0) {
            throw new IOException("QR code generation failed for " + packageDTO.getReference());
        }
        Path tempLabelFile = Paths.get(tempLabelPath);
        Files.deleteIfExists(tempLabelFile);
        PDFGenerator.generatePdf(packageDTO, qrPath, tempLabelPath, locale);
        if (!Files.exists(tempLabelFile) || Files.size(tempLabelFile) == 0) {
            throw new IOException("PDF label generation failed for " + packageDTO.getReference());
        }
        Files.move(tempLabelFile, Paths.get(labelPath), StandardCopyOption.REPLACE_EXISTING);
    }

    private String buildPackageConsultationLink(String packageReference) {
        return resolveFrontendBaseUrl() + "/package?id=" + packageReference;
    }

    private String buildPackageTrackingLink(String packageReference) {
        return resolveFrontendBaseUrl() + "/packageTracking?packageReference=" + packageReference;
    }

    private String buildEvaluationLink(String packageReference) {
        return resolveFrontendBaseUrl() + "/evaluate/" + packageReference;
    }

    private String resolveFrontendBaseUrl() {
        String configuredBaseUrl = frontendBaseUrl;
        if (configuredBaseUrl == null || configuredBaseUrl.isBlank()) {
            configuredBaseUrl = getPackageByIdURL;
        }
        if (configuredBaseUrl != null && configuredBaseUrl.contains("/package?id=")) {
            configuredBaseUrl = configuredBaseUrl.substring(0, configuredBaseUrl.indexOf("/package?id="));
        }
        return PublicFrontendUrlResolver.resolvePreferredBaseUrl(configuredBaseUrl);
    }

    @Override
    public void createNewPackages(List<PackageDTO> packageDTOS, Locale locale) {
        packageDTOS.stream().forEach(packageDTO -> {
            try {
                packageAddressGeocoding(packageDTO);
                DistanceMatrix distanceMatrix = packageDistanceCalculation(packageDTO);
                preparPackage(packageDTO, locale, calculatePricingBreakdown(packageDTO, distanceMatrix));
            } catch (MalformedURLException | FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<PackageDTO>> getPAckagesAroundPosition(String latitude, String longitude, double rayonEnMetres) {
        List<Address> addresses = packages.findAddressAroundPosition(latitude,longitude,rayonEnMetres);
        List<PackageDTO> packageDTOS = addresses.stream()
                .map(address -> {
                    Package aPackage = address.getPackaged();
                    PackageDTO packageDTO = toPackageDTO(aPackage);
                    return packageDTO;
                })
                .collect(Collectors.toList());
        Map<String, List<PackageDTO>> groupedPackages = packageDTOS.parallelStream()
                .collect(Collectors.groupingByConcurrent(packaged -> {
                    AddressDTO departureAddress = getDepartureAddress(packaged.getAddresses());
                    DistanceMatrix distancePackageUser = GeoHelper.getDistanceByCoordinates(geoApiContext, departureAddress.getLatitude().doubleValue(),
                            departureAddress.getLongitude().doubleValue()
                            , Double.parseDouble(latitude), Double.parseDouble(longitude));
                    return departureAddress.getLatitude()+","+departureAddress.getLongitude()+","+departureAddress.toString()
                            +" ("+formatDistanceMatrixValue(distancePackageUser)+")";
                }));
        return groupedPackages;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<PackageDTO>> getPAckagesAroundPosition(AddressDTO address, double rayonEnMetres) throws IOException, InterruptedException, ApiException {
        GeoHelper.AddressGeoCoding(geoApiContext,address);
        return getPAckagesAroundPosition(address.getLatitude().toString(), address.getLongitude().toString(), rayonEnMetres);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageDTO> getPackagesAroundPosition(String latitude, String longitude, double rayonEnMetres) {
        List<Address> addresses = packages.findAddressAroundPosition(latitude,longitude,rayonEnMetres);
        List<PackageDTO> packageDTOS = addresses.stream()
                .map(address -> {
                    Package aPackage = address.getPackaged();
                    PackageDTO packageDTO = toPackageDTO(aPackage);
                    AddressDTO departureAddress = getDepartureAddress(packageDTO.getAddresses());
                    DistanceMatrix distancePackageUser = GeoHelper.getDistanceByCoordinates(geoApiContext, departureAddress.getLatitude().doubleValue(),
                            departureAddress.getLongitude().doubleValue()
                            , Double.parseDouble(latitude), Double.parseDouble(longitude));
                    packageDTO.setFromYou(formatDistanceMatrixValue(distancePackageUser));
                    return packageDTO;
                })
                .collect(Collectors.toList());
        return packageDTOS;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageDTO> getPackagesAroundPositionWithDestination(String latitude, String longitude, AddressDTO destinationAddress, double rayonEnMetres) throws IOException, InterruptedException, ApiException {
        GeoHelper.AddressGeoCoding(geoApiContext, destinationAddress);
        if (destinationAddress.getLatitude() == null || destinationAddress.getLongitude() == null) {
            return Collections.emptyList();
        }

        return getPackagesAroundPosition(latitude, longitude, rayonEnMetres).stream()
                .filter(packageDTO -> {
                    AddressDTO arrivalAddress = getArrivalAddress(packageDTO.getAddresses());
                    if (arrivalAddress == null || arrivalAddress.getLatitude() == null || arrivalAddress.getLongitude() == null) {
                        return false;
                    }
                    return haversineMeters(
                            arrivalAddress.getLatitude().doubleValue(),
                            arrivalAddress.getLongitude().doubleValue(),
                            destinationAddress.getLatitude().doubleValue(),
                            destinationAddress.getLongitude().doubleValue()
                    ) <= rayonEnMetres;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageDTO> findAddressOnMyRoad(String departureLatitude, String arrivalLatitude, String departureLongitude, String arrivalLongitude) {
        double startLat = Double.parseDouble(departureLatitude);
        double startLng = Double.parseDouble(departureLongitude);
        double endLat = Double.parseDouble(arrivalLatitude);
        double endLng = Double.parseDouble(arrivalLongitude);

        double routeDistanceMeters = haversineMeters(startLat, startLng, endLat, endLng);
        if (routeDistanceMeters < 50d) {
            return Collections.emptyList();
        }

        // Dynamic corridor width: adaptive to route length but bounded for reliability.
        double corridorMeters = Math.max(700d, Math.min(routeDistanceMeters * 0.12d, 3500d));
        double corridorLatMargin = metersToLatitudeDelta(corridorMeters);
        double corridorLngMargin = metersToLongitudeDelta(corridorMeters, (startLat + endLat) / 2d);

        double minLat = Math.min(startLat, endLat) - corridorLatMargin;
        double maxLat = Math.max(startLat, endLat) + corridorLatMargin;
        double minLng = Math.min(startLng, endLng) - corridorLngMargin;
        double maxLng = Math.max(startLng, endLng) + corridorLngMargin;

        List<Package> candidatePackages = this.packages.findNewPackagesInBoundingBox(minLat, maxLat, minLng, maxLng);

        return candidatePackages.stream()
                .filter(pkg -> pkg.getAddresses() != null && pkg.getAddresses().size() >= 2)
                .map(pkg -> {
                    Address dep = getDepartureAddress(pkg.getAddresses());
                    Address arr = getArrivalAddress(pkg.getAddresses());
                    if (dep == null || arr == null || dep.getLatitude() == null || dep.getLongitude() == null
                            || arr.getLatitude() == null || arr.getLongitude() == null) {
                        return null;
                    }
                    GeoPoint routeStart = new GeoPoint(startLat, startLng);
                    GeoPoint routeEnd = new GeoPoint(endLat, endLng);
                    GeoPoint depPoint = new GeoPoint(dep.getLatitude().doubleValue(), dep.getLongitude().doubleValue());
                    GeoPoint arrPoint = new GeoPoint(arr.getLatitude().doubleValue(), arr.getLongitude().doubleValue());

                    double depDistanceToRoute = pointToSegmentDistanceMeters(depPoint, routeStart, routeEnd);
                    double arrDistanceToRoute = pointToSegmentDistanceMeters(arrPoint, routeStart, routeEnd);
                    if (depDistanceToRoute > corridorMeters || arrDistanceToRoute > corridorMeters * 1.25d) {
                        return null;
                    }

                    double depProgress = projectionProgress(depPoint, routeStart, routeEnd);
                    double arrProgress = projectionProgress(arrPoint, routeStart, routeEnd);
                    if (depProgress < -0.05d || depProgress > 1.05d || arrProgress < -0.05d || arrProgress > 1.15d) {
                        return null;
                    }
                    if (depProgress > arrProgress) {
                        return null;
                    }
                    // Avoid packages that mostly go backward relative to the current trip direction.
                    if (arrProgress < 0.10d) {
                        return null;
                    }

                    PackageDTO dto = toPackageDTO(pkg);
                    return dto;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(PackageDTO::getId))
                .collect(Collectors.toList());
    }

    @Override
    public PackageReservation reservePackage(Long packageID, Long deliveryPersonID, Locale locale) throws NoSuchAlgorithmException {
        return recordPackageOperationChecked("reserve", () -> {
            PackageReservation packageReservation = reservePackageInternal(packageID, deliveryPersonID);
            Package aPackage = packageReservation.getaPackage();
            CompletableFuture.runAsync(() -> {
                sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_RESERVATION_SENDER.getType(), messageSource.getMessage("email.subject.packageReserved", null, locale),EMAIL_TYPE.PACKAGE_RESERVATION_SENDER);
                sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_RESERVATION_DELIVERY.getType(), messageSource.getMessage("email.subject.packageReservationCofirm", null, locale),EMAIL_TYPE.PACKAGE_RESERVATION_DELIVERY);
            }, packageMailTaskExecutor);
            return packageReservation;
        });
    }

    @Override
    public ReserveBatchResultDTO reservePackagesBatch(List<Long> packageIds, Long deliveryPersonID, Locale locale) {
        return recordPackageOperation("reserveBatch", () -> {
            ReserveBatchResultDTO result = new ReserveBatchResultDTO();
            result.setDeliveryPersonId(deliveryPersonID);

            if (packageIds == null || packageIds.isEmpty()) {
                result.setRequestedCount(0);
                result.setReservedCount(0);
                return result;
            }

            LinkedHashSet<Long> uniqueIds = packageIds.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            result.setRequestedCount(uniqueIds.size());

            for (Long packageId : uniqueIds) {
                try {
                    PackageReservation reservation = reservePackageInternal(packageId, deliveryPersonID);
                    Package aPackage = reservation.getaPackage();
                    CompletableFuture.runAsync(() -> {
                        sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_RESERVATION_SENDER.getType(),
                                messageSource.getMessage("email.subject.packageReserved", null, locale), EMAIL_TYPE.PACKAGE_RESERVATION_SENDER);
                        sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_RESERVATION_DELIVERY.getType(),
                                messageSource.getMessage("email.subject.packageReservationCofirm", null, locale), EMAIL_TYPE.PACKAGE_RESERVATION_DELIVERY);
                    }, packageMailTaskExecutor);
                    result.getReservedPackageIds().add(packageId);
                } catch (ResponseStatusException ex) {
                    result.getSkippedPackages().put(packageId, ex.getReason() == null ? ex.getStatusCode().toString() : ex.getReason());
                } catch (Exception ex) {
                    result.getSkippedPackages().put(packageId, "Unexpected error while reserving package");
                }
            }

            result.setReservedCount(result.getReservedPackageIds().size());
            return result;
        });
    }

    @Override
    public void pickUpPackage(Long packageID, Long deliveryPersonID, String pickUpOTP, Locale locale) throws NoSuchAlgorithmException {
        recordPackageOperationChecked("pickup", () -> {
            Package aPackage = packages.findById(packageID)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
            Optional<PackageReservation> packageReservation = aPackage.getPackageReservations().stream()
                    .filter(currentReservation -> currentReservation.getDeliveryPerson() != null
                            && currentReservation.getDeliveryPerson().getId().equals(deliveryPersonID))
                    .findFirst();
            if(packageReservation.isPresent() && Objects.equals(packageReservation.get().getPickUpOTP(), pickUpOTP)) {
                aPackage.setStatus(PACKAGE_STATUS.PICKEDUP);
                packageReservation.get().setDeliveryOTP(OTPHelper.generateOTP(OTPSecret, System.currentTimeMillis()));
                packages.saveAndFlush(aPackage);
                CompletableFuture.runAsync(() -> {
                    sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_PICKUP_SENDER.getType(),
                            messageSource.getMessage("email.subject.packagePickup", null, locale),EMAIL_TYPE.PACKAGE_PICKUP_SENDER);
                    sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_PICKUP_RECEIVER.getType(),
                            messageSource.getMessage("email.subject.packagePickupR", null, locale),EMAIL_TYPE.PACKAGE_PICKUP_RECEIVER);
                    sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_PICKUP_DELIVERY.getType(),
                            messageSource.getMessage("email.subject.packagePickupCofirm", null, locale),EMAIL_TYPE.PACKAGE_PICKUP_DELIVERY);
                }, packageMailTaskExecutor);
                return null;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid pickup OTP");
        });
    }

    @Override
    public void deliverPackage(Long packageID, Long deliveryPersonID, String pickUpOTP, Locale locale) throws NoSuchAlgorithmException {
        recordPackageOperationChecked("deliver", () -> {
            Package aPackage = packages.findById(packageID)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
            Optional<PackageReservation> packageReservation = aPackage.getPackageReservations().stream()
                    .filter(currentReservation -> currentReservation.getDeliveryPerson() != null
                            && currentReservation.getDeliveryPerson().getId().equals(deliveryPersonID))
                    .findFirst();
            if(packageReservation.isPresent() && Objects.equals(packageReservation.get().getDeliveryOTP(), pickUpOTP)) {
                aPackage.setStatus(PACKAGE_STATUS.DELIVERED);
                packageReservation.get().setStatus(PACKAGE_RESERVATION_STATUS.FINISHED);
                packages.saveAndFlush(aPackage);
                createCourierPayoutIfNeeded(aPackage, packageReservation.get().getDeliveryPerson());
                CompletableFuture.runAsync(() -> {
                    sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_DELIVERY_RECEIVER.getType(),
                            messageSource.getMessage("email.subject.packageDelivered", null, locale),EMAIL_TYPE.PACKAGE_DELIVERY_RECEIVER);
                    sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_DELIVERY_SENDER.getType(),
                            messageSource.getMessage("email.subject.packageDelivered", null, locale),EMAIL_TYPE.PACKAGE_DELIVERY_SENDER);
                }, packageMailTaskExecutor);
                return null;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid delivery OTP");
        });
    }

    @Override
    @Transactional(readOnly = true)
    public CHECK_STATUS checkOTPForPickUpPackage(Long packageID, Long senderID, String pickUpOTP) {
        Package aPackage = packages.findById(packageID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
        List<PackageReservation> packageReservation_ = aPackage.getPackageReservations().stream().filter(packageReservation -> packageReservation.getPickUpOTP().equals(pickUpOTP)).collect(Collectors.toList());
        if(Boolean.TRUE.equals(aPackage.getGuestMode())){
            return CHECK_STATUS.KO;
        }
        if(packageReservation_.size()>0 && aPackage.getSender() != null && aPackage.getSender().getId().equals(senderID)) {
           return CHECK_STATUS.OK;
        }else{
            return CHECK_STATUS.KO;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CHECK_STATUS checkOTPForDeliverPackage(Long packageID, Long deliveryPersonID, String deliveryOTP) {
        Package aPackage = packages.findById(packageID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
        List<PackageReservation> packageReservation_ = aPackage.getPackageReservations().stream().filter(packageReservation -> packageReservation.getDeliveryOTP().equals(deliveryOTP)).collect(Collectors.toList());
        if(Boolean.TRUE.equals(aPackage.getGuestMode())){
            return CHECK_STATUS.KO;
        }
        if(packageReservation_.size()>0 && aPackage.getSender() != null && aPackage.getSender().getId().equals(deliveryPersonID)) {
            return CHECK_STATUS.OK;
        }else{
            return CHECK_STATUS.KO;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CHECK_STATUS checkGuestOTPForPickUpPackage(Long packageID, String guestAccessToken, String pickUpOTP) {
        Package aPackage = packages.findById(packageID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
        List<PackageReservation> packageReservation_ = aPackage.getPackageReservations().stream()
                .filter(packageReservation -> packageReservation.getPickUpOTP().equals(pickUpOTP))
                .collect(Collectors.toList());
        if (packageReservation_.size() > 0 && isValidGuestToken(aPackage, guestAccessToken)) {
            return CHECK_STATUS.OK;
        }
        return CHECK_STATUS.KO;
    }

    @Override
    @Transactional(readOnly = true)
    public CHECK_STATUS checkGuestOTPForDeliverPackage(Long packageID, String guestAccessToken, String deliveryOTP) {
        Package aPackage = packages.findById(packageID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
        List<PackageReservation> packageReservation_ = aPackage.getPackageReservations().stream()
                .filter(packageReservation -> packageReservation.getDeliveryOTP().equals(deliveryOTP))
                .collect(Collectors.toList());
        if (packageReservation_.size() > 0 && isValidGuestToken(aPackage, guestAccessToken)) {
            return CHECK_STATUS.OK;
        }
        return CHECK_STATUS.KO;
    }

    @Override
    @Transactional
    public void confirmGuestPackagePayment(Long packageID, String guestAccessToken) {
        Package aPackage = packages.findById(packageID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
        if (!isValidGuestToken(aPackage, guestAccessToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid guest access token");
        }
        if (!PACKAGE_STATUS.PAYMENTPENDING.equals(aPackage.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Package is not waiting for payment");
        }
        packages.updatePackagesStatus(PACKAGE_STATUS.NEW, packageID);
    }

    @Override
    @Transactional(readOnly = true)
    public PackageDTO findPackageByID(Long id) {
        Package aPackage = packages.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
        return toPackageDTO(aPackage);
    }

    @Override
    public PackageDTO updatePackage(PackageDTO user) {
        return null;
    }

    @Override
    public void deletePackage(PackageDTO user) {

    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageDTO> findPackagesByStatus(PACKAGE_STATUS status) {
        List<PackageDTO> packageDTOS = new ArrayList<>();
        List<Package> packages = this.packages.findPackagesByStatus(status);
        packages.stream().forEach(aPackage -> packageDTOS.add(toPackageDTO(aPackage)));
        return packageDTOS;
    }

    @Override
    @Transactional
    public void updatePackageStatus(PACKAGE_STATUS status, Long id) {
        packages.updatePackagesStatus(status,id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<PACKAGE_STATUS, List<PackageDTO>> getPackagesByDeliveryPerson(Long deliveryPersonID) {
        List<Package> packageList = packages.findPackagesByDeliveryPerson(deliveryPersonID);
        List<PackageDTO> packageDTOS = packageList.stream()
                .map(this::toPackageDTO)
                .collect(Collectors.toList());
        Map<PACKAGE_STATUS, List<PackageDTO>> groupedPackages = packageDTOS.parallelStream()
                .collect(Collectors.groupingByConcurrent(packaged -> {
                    return packaged.getStatus();
                }));
        return groupedPackages;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<PACKAGE_STATUS, List<PackageDTO>> getPackagesBySender(Long senderID) {
        List<Package> packageList = packages.findPackagesBySender(senderID);
        List<PackageDTO> packageDTOS = packageList.stream()
                .map(this::toPackageDTO)
                .collect(Collectors.toList());
        return packageDTOS.parallelStream()
                .collect(Collectors.groupingByConcurrent(PackageDTO::getStatus));
    }

    private Package preparPackage(PackageDTO packageDTO, Locale locale, PackagePricingBreakdown pricingBreakdown) throws MalformedURLException, FileNotFoundException {
        applyPricing(packageDTO, pricingBreakdown);
        Package aPackage = modelMapper.map(packageDTO, Package.class);
        User sender = resolveSender(packageDTO);
        boolean guestMode = sender == null;
        StringBuffer reference = new StringBuffer();
        attachPackageSettlement(aPackage, pricingBreakdown);
        aPackage.getAddresses().stream().forEach(address -> address.setPackaged(aPackage));
        aPackage.setSender(sender);
        aPackage.setGuestMode(guestMode);
        aPackage.setGuestAccessToken(UUID.randomUUID().toString());
        aPackage.setCreationDate(Timestamp.valueOf(LocalDateTime.now()));
        reference.append(packageReferenceStart)
                .append(resolveCountryCodePrefix(packageDTO))
                .append(aPackage.getCreationDate().toString().replaceAll("[\\s\\-:.]", ""));
        aPackage.setReference(reference.toString());
        packageDTO.setReference(reference.toString());
        packageDTO.setCreationDate(aPackage.getCreationDate());
        packageDTO.setGuestMode(guestMode);
        packageDTO.setGuestAccessToken(aPackage.getGuestAccessToken());
        //GeoHelper.getDirection(geoApiContext, getDepartureAddress(packageDTO.getAddresses()).toString(), getArrivalAddress(packageDTO.getAddresses()).toString());
        packageDTO.setId(aPackage.getId());
        packageDTO.setVersion(aPackage.getVersion());
        Document qrDocument = new Document();
        qrDocument.setaPackage(aPackage);
        qrDocument.setType(DOCUMENT_TYPE.PACKAGE_QR);
        qrDocument.setDocURL(packagesDirectory+packageDTO.getReference()+packageQrEnds);
        aPackage.getDocument().add(qrDocument);
        Document pdfDocument = new Document();
        pdfDocument.setaPackage(aPackage);
        pdfDocument.setType(DOCUMENT_TYPE.PACKAGE_PDF_LABEL);
        pdfDocument.setDocURL(packagesDirectory+packageDTO.getReference()+packageLabelEnds);
        aPackage.getDocument().add(pdfDocument);
        return aPackage;
    }

    private void applyPricing(PackageDTO packageDTO, PackagePricingBreakdown pricingBreakdown) {
        packageDTO.setDeliveryPrice(pricingBreakdown.customerTotalPrice());
        packageDTO.setCustomerTotalPrice(pricingBreakdown.customerTotalPrice());
        packageDTO.setDeliveryBaseAmount(pricingBreakdown.deliveryBaseAmount());
        packageDTO.setInsuranceFee(pricingBreakdown.insuranceFee());
        packageDTO.setPlatformServiceFee(pricingBreakdown.platformServiceFee());
        packageDTO.setDeliveryRevenueExcludingServiceFee(pricingBreakdown.deliveryRevenueExcludingServiceFee());
        packageDTO.setPlatformCommissionRate(pricingBreakdown.platformCommissionRate());
        packageDTO.setPlatformCommissionAmount(pricingBreakdown.platformCommissionAmount());
        packageDTO.setCourierShareRate(pricingBreakdown.courierShareRate());
        packageDTO.setCourierPayoutAmount(pricingBreakdown.courierPayoutAmount());
        packageDTO.setCurrency(pricingBreakdown.currency());
        packageDTO.setPricingVersion(pricingBreakdown.pricingVersion());
        packageDTO.setCalculatedAt(Timestamp.valueOf(LocalDateTime.now()));
    }

    private void attachPackageSettlement(Package aPackage, PackagePricingBreakdown pricingBreakdown) {
        aPackage.setDeliveryPrice(pricingBreakdown.customerTotalPrice());
        PackageSettlement settlement = aPackage.getPackageSettlement();
        if (settlement == null) {
            settlement = new PackageSettlement();
            settlement.setaPackage(aPackage);
        }
        settlement.setCustomerTotalPrice(pricingBreakdown.customerTotalPrice());
        settlement.setDeliveryBaseAmount(pricingBreakdown.deliveryBaseAmount());
        settlement.setInsuranceFee(pricingBreakdown.insuranceFee());
        settlement.setPlatformServiceFee(pricingBreakdown.platformServiceFee());
        settlement.setDeliveryRevenueExcludingServiceFee(pricingBreakdown.deliveryRevenueExcludingServiceFee());
        settlement.setPlatformCommissionRate(pricingBreakdown.platformCommissionRate());
        settlement.setPlatformCommissionAmount(pricingBreakdown.platformCommissionAmount());
        settlement.setCourierShareRate(pricingBreakdown.courierShareRate());
        settlement.setCourierPayoutAmount(pricingBreakdown.courierPayoutAmount());
        settlement.setCurrency(pricingBreakdown.currency());
        settlement.setPricingVersion(pricingBreakdown.pricingVersion());
        settlement.setCalculatedAt(Timestamp.valueOf(LocalDateTime.now()));
        aPackage.setPackageSettlement(settlement);
    }

    private void packageAddressGeocoding(PackageDTO aPackage){
        aPackage.getAddresses().stream().forEach(addressDTO -> {
            if (addressDTO.getLatitude() != null && addressDTO.getLongitude() != null) {
                return;
            }
            try {
                GeoHelper.AddressGeoCoding(geoApiContext, addressDTO);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ApiException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private DistanceMatrix packageDistanceCalculation(PackageDTO aPackage){
        DistanceMatrix distancePackageDestination = GeoHelper.getDistanceByAddress(geoApiContext, getDepartureAddress(aPackage.getAddresses()).toString(),
                getArrivalAddress(aPackage.getAddresses()).toString());
        aPackage.setDistanceToDestination(formatDistanceMatrixValue(distancePackageDestination));
        return distancePackageDestination;
    }

    private PackagePricingBreakdown calculatePricingBreakdown(PackageDTO packageDTO, DistanceMatrix distanceMatrix) {
        return PackageDeliveryPriceCalculator.calculatePricingBreakdown(extractDistanceInKilometers(distanceMatrix), packageDTO);
    }

    private double extractDistanceInKilometers(DistanceMatrix distanceMatrix) {
        if (distanceMatrix == null
                || distanceMatrix.rows == null
                || distanceMatrix.rows.length == 0
                || distanceMatrix.rows[0] == null
                || distanceMatrix.rows[0].elements == null
                || distanceMatrix.rows[0].elements.length == 0
                || distanceMatrix.rows[0].elements[0] == null
                || distanceMatrix.rows[0].elements[0].distance == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to resolve route distance");
        }
        return distanceMatrix.rows[0].elements[0].distance.inMeters / 1000.0;
    }

    private void validatePackageCreationRequest(PackageDTO packageDTO) {
        if (packageDTO == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Package payload is required");
        }
        if (packageDTO.getAddresses() == null || packageDTO.getAddresses().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pickup and delivery addresses are required");
        }
        AddressDTO departureAddress = getDepartureAddress(packageDTO.getAddresses());
        AddressDTO arrivalAddress = getArrivalAddress(packageDTO.getAddresses());
        if (departureAddress == null || arrivalAddress == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pickup and delivery addresses are required");
        }
        validateInsuranceSelection(packageDTO);
        validateFloorAccessibility(departureAddress, "pickup");
        validateFloorAccessibility(arrivalAddress, "delivery");
        validateDistinctAddresses(departureAddress, arrivalAddress);

        boolean guestMode = packageDTO.getSenderID() == null;
        packageDTO.setGuestMode(guestMode);
        if (guestMode) {
            validateGuestAddressContact(departureAddress, "pickup");
        }
        validateGuestAddressContact(arrivalAddress, "delivery");
    }

    private void validateGuestAddressContact(AddressDTO address, String label) {
        if (isBlank(address.getFirstName()) || isBlank(address.getLastName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contact name is required for " + label);
        }
        if (isBlank(address.getPhone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is required for " + label);
        }
        if ("pickup".equals(label) && isBlank(address.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required for pickup");
        }
    }

    private void validateInsuranceSelection(PackageDTO packageDTO) {
        if (Boolean.TRUE.equals(packageDTO.getInsuranceSelected())
                && (packageDTO.getDeclaredValue() == null || packageDTO.getDeclaredValue() <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Declared value is required when insurance is selected");
        }
    }

    private void validateFloorAccessibility(AddressDTO address, String label) {
        if (address == null) {
            return;
        }
        int floor = address.getFloor() == null ? 0 : Math.max(address.getFloor(), 0);
        if (floor > 0 && address.getHasElevator() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elevator selection is required for " + label);
        }
    }

    private void validateDistinctAddresses(AddressDTO departureAddress, AddressDTO arrivalAddress) {
        if (departureAddress == null || arrivalAddress == null) {
            return;
        }

        String normalizedDeparture = normalizeAddressSignature(departureAddress);
        String normalizedArrival = normalizeAddressSignature(arrivalAddress);
        if (!normalizedDeparture.isBlank() && normalizedDeparture.equals(normalizedArrival)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivery address must be different from pickup address");
        }
    }

    private String normalizeAddressSignature(AddressDTO address) {
        return String.join("|",
                        normalizeAddressPart(address.getLine1()),
                        normalizeAddressPart(address.getZipCode()),
                        normalizeAddressPart(address.getTown()),
                        normalizeAddressPart(address.getCountry()),
                        normalizeAddressPart(address.getAddressAuto()))
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeAddressPart(String value) {
        return value == null ? "" : value.trim();
    }

    private User resolveSender(PackageDTO packageDTO) {
        if (packageDTO.getSenderID() == null) {
            return null;
        }
        return users.findById(packageDTO.getSenderID())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sender not found"));
    }

    private boolean isValidGuestToken(Package aPackage, String guestAccessToken) {
        return Boolean.TRUE.equals(aPackage.getGuestMode())
                && guestAccessToken != null
                && Objects.equals(aPackage.getGuestAccessToken(), guestAccessToken);
    }

    private String buildFollowupLink(Package aPackage) {
        if (aPackage.getGuestAccessToken() != null) {
            return buildPackageTrackingLink(aPackage.getReference()) + "&guestAccessToken=" + aPackage.getGuestAccessToken();
        }
        return buildPackageTrackingLink(aPackage.getReference());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String resolveCountryCodePrefix(PackageDTO packageDTO) {
        if (packageDTO.getAddresses() == null || packageDTO.getAddresses().isEmpty()) {
            return "XX";
        }
        String country = packageDTO.getAddresses().get(0).getCountry();
        if (country == null) {
            return "XX";
        }
        String normalizedCountry = country.trim().toUpperCase(Locale.ROOT);
        if (normalizedCountry.length() >= 2) {
            return normalizedCountry.substring(0, 2);
        }
        if (normalizedCountry.length() == 1) {
            return normalizedCountry + "X";
        }
        return "XX";
    }

    private String formatDistanceMatrixValue(DistanceMatrix distanceMatrix) {
        if (distanceMatrix == null
                || distanceMatrix.rows == null
                || distanceMatrix.rows.length == 0
                || distanceMatrix.rows[0] == null
                || distanceMatrix.rows[0].elements == null
                || distanceMatrix.rows[0].elements.length == 0
                || distanceMatrix.rows[0].elements[0] == null
                || distanceMatrix.rows[0].elements[0].duration == null
                || distanceMatrix.rows[0].elements[0].distance == null) {
            return "-/-";
        }
        return distanceMatrix.rows[0].elements[0].duration + "/" + distanceMatrix.rows[0].elements[0].distance;
    }

    private void sendPackageCreationEMail(Package aPackage, Locale locale, String template, String subject, EMAIL_TYPE type){
        Map<String, Object> templateModel = new HashMap<>();
        Address departureAddress = getDepartureAddress(aPackage.getAddresses());
        Address arrivalAddress = getArrivalAddress(aPackage.getAddresses());
        if(type.equals(EMAIL_TYPE.PACKAGE_CREATION) || type.equals(EMAIL_TYPE.PACKAGE_RESERVATION_SENDER) || type.equals(EMAIL_TYPE.PACKAGE_PICKUP_SENDER)
                ||type.equals(EMAIL_TYPE.PACKAGE_DELIVERY_SENDER)) {
            templateModel.put("recipientName", departureAddress.getFirstName() + " " + departureAddress.getLastName());
        }else if(type.equals(EMAIL_TYPE.PACKAGE_PICKUP_RECEIVER) ||type.equals(EMAIL_TYPE.PACKAGE_DELIVERY_RECEIVER)) {
            templateModel.put("recipientName", arrivalAddress.getFirstName() + " " + arrivalAddress.getLastName());
        }
        templateModel.put("height", aPackage.getHeight());
        templateModel.put("width", aPackage.getWidth());
        templateModel.put("depth", aPackage.getDepth());
        templateModel.put("weight", aPackage.getWeight());
        templateModel.put("packageReference", aPackage.getReference());
        templateModel.put("deliveryPrice", aPackage.getDeliveryPrice());
        templateModel.put("followupLink", buildFollowupLink(aPackage));
        templateModel.put("evaluationLink", buildEvaluationLink(aPackage.getReference()));
        templateModel.put("guestMode", Boolean.TRUE.equals(aPackage.getGuestMode()));
        templateModel.put("guestAccessToken", aPackage.getGuestAccessToken());
        if(aPackage.getPackageReservations() != null && aPackage.getPackageReservations().size()>0) {
            aPackage.getPackageReservations().stream().forEach(packageReservation -> {
                if (packageReservation.getStatus().equals(PACKAGE_RESERVATION_STATUS.ONGOING)) {
                    templateModel.put("otp", packageReservation.getPickUpOTP());
                    templateModel.put("deliveryOtp", packageReservation.getDeliveryOTP());
                    if(type.equals(EMAIL_TYPE.PACKAGE_RESERVATION_DELIVERY)||type.equals(EMAIL_TYPE.PACKAGE_PICKUP_DELIVERY)) {
                        templateModel.put("recipientName", packageReservation.getDeliveryPerson().getFirstName() + " " + packageReservation.getDeliveryPerson().getLastName());
                    }
                }
            });
        }
        templateModel.put("deliveryPrice", aPackage.getDeliveryPrice());
        templateModel.put("departureAddress", departureAddress.formatedtoString());
        templateModel.put("pickupDateTime", departureAddress.getDateTime());
        templateModel.put("arrivalAddress", arrivalAddress.formatedtoString());
        templateModel.put("deliveryDateTime", arrivalAddress.getDateTime());
        String attachement;
        if(type.equals(EMAIL_TYPE.PACKAGE_RESERVATION_DELIVERY) || type.equals(EMAIL_TYPE.PACKAGE_PICKUP_DELIVERY) || type.equals(EMAIL_TYPE.PACKAGE_PICKUP_RECEIVER)
            || type.equals(EMAIL_TYPE.PACKAGE_PICKUP_SENDER) || type.equals(EMAIL_TYPE.PACKAGE_DELIVERY_RECEIVER) || type.equals(EMAIL_TYPE.PACKAGE_DELIVERY_SENDER)){
            attachement = null;
        }else{
            attachement = packagesDirectory+aPackage.getReference()+packageLabelEnds;
            if (!Files.exists(Paths.get(attachement))) {
                logger.warn("Skipping missing label attachment for package {}: {}", aPackage.getReference(), attachement);
                attachement = null;
            }
        }
        String recipientEmail = resolveRecipientEmail(aPackage, departureAddress, arrivalAddress, type);
        if (isBlank(recipientEmail)) {
            logger.warn("Skipping package email {} for package {} because recipient email is missing", type, aPackage.getReference());
            return;
        }
        try {
            MailHelper.sendMessageUsingThymeleafTemplate(messageSource,templateResolver,recipientEmail,
                    subject,templateModel, locale, template,attachement);
        } catch (Exception e) {
            logger.warn("Unable to send package email {} for package {}: {}", type, aPackage.getReference(), e.getMessage(), e);
        }
    }

    private String resolveRecipientEmail(Package aPackage, Address departureAddress, Address arrivalAddress, EMAIL_TYPE type) {
        return switch (type) {
            case PACKAGE_PICKUP_RECEIVER, PACKAGE_DELIVERY_RECEIVER -> arrivalAddress == null ? null : arrivalAddress.getEmail();
            case PACKAGE_PICKUP_DELIVERY, PACKAGE_RESERVATION_DELIVERY -> resolveDeliveryPersonEmail(aPackage);
            default -> departureAddress == null ? null : departureAddress.getEmail();
        };
    }

    private String resolveDeliveryPersonEmail(Package aPackage) {
        if (aPackage.getPackageReservations() == null) {
            return null;
        }

        return aPackage.getPackageReservations().stream()
                .filter(packageReservation -> packageReservation.getStatus().equals(PACKAGE_RESERVATION_STATUS.ONGOING))
                .map(PackageReservation::getDeliveryPerson)
                .filter(Objects::nonNull)
                .map(User::getEmailAddress)
                .filter(email -> email != null && !email.isBlank())
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Address> findUsersAroundPosition(String aPackage){
        Address address = getDepartureAddress(packages.findPackageByReference(aPackage).getAddresses());
        return users.findUsersAroundPosition(address.getLatitude().toString(), address.getLongitude().toString(), 20000);
    }

    @Override
    @Transactional(readOnly = true)
    public PackageDTO findPackageByReference(String reference) {
        return recordPackageOperation("findByReference", () -> {
            if (reference == null || reference.isBlank() || "undefined".equalsIgnoreCase(reference)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Package reference is required");
            }
            Package aPackage = packages.findPackageByReference(reference);
            if (aPackage == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found for reference: " + reference);
            }
            return toPackageDTO(aPackage);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public PackageDTO findGuestPackageByReference(String reference, String guestAccessToken) {
        PackageDTO packageDTO = findPackageByReference(reference);
        if (!Objects.equals(packageDTO.getGuestAccessToken(), guestAccessToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid guest access token");
        }
        return packageDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentContentDTO loadPackageDocumentContent(Long documentId) {
        return recordPackageOperation("documentContent", () -> {
            Document document = documents.findById(documentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
            if (!isPackageDocument(document)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported package document type");
            }
            DocumentContentDTO contentDTO = new DocumentContentDTO();
            try {
                byte[] data = Files.readAllBytes(Paths.get(document.getDocURL()));
                recordPackageDocumentContentSize(data.length);
                contentDTO.setData(data);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file not found");
            }
            contentDTO.setFileName(resolveDocumentFileName(document));
            contentDTO.setContentType(resolveDocumentContentType(document));
            return contentDTO;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserWithOngoingDelivery(Long userId) {
        return packages.existsOngoingReservationsForUserWithPickedUpPackage(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceMetricsDTO loadAdminMetrics() {
        ServiceMetricsDTO metrics = new ServiceMetricsDTO();
        metrics.setServiceName("packages-service");
        metrics.setUptimeSeconds(readGauge("process.uptime"));
        metrics.setHeapUsedMb(toMegabytes(readGauge("jvm.memory.used", "area", "heap")));
        metrics.setHeapMaxMb(toMegabytes(readGauge("jvm.memory.max", "area", "heap")));
        metrics.setCpuUsagePercent(toPercent(readGauge("system.cpu.usage")));
        metrics.setHttpRequestCount(sumMetric("http.server.requests"));
        metrics.setOperationCallCount(sumMetric("quickdelivery.packages.operation.calls"));
        metrics.setAsyncQueueSize(sumMetric("quickdelivery.async.queue.size"));
        metrics.setAsyncActiveCount(sumMetric("quickdelivery.async.active.count"));
        metrics.setOcrProcessedCount(null);
        return metrics;
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceHttpBreakdownDTO loadAdminHttpBreakdown() {
        ServiceHttpBreakdownDTO breakdown = new ServiceHttpBreakdownDTO();
        breakdown.setServiceName("packages-service");

        Map<String, HttpEndpointMetricDTO> aggregated = new LinkedHashMap<>();
        meterRegistry.find("http.server.requests").meters().forEach(meter -> collectHttpEndpointMetric(aggregated, meter));

        List<HttpEndpointMetricDTO> endpoints = aggregated.values().stream()
                .sorted(Comparator.comparingDouble(HttpEndpointMetricDTO::getRequestCount).reversed())
                .limit(12)
                .toList();
        breakdown.setEndpoints(endpoints);
        breakdown.setTotalRequestCount(aggregated.values().stream().mapToDouble(HttpEndpointMetricDTO::getRequestCount).sum());
        return breakdown;
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialDashboardDTO loadAdminFinancialDashboard() {
        List<PackageSettlement> settlements = packageSettlements.findAllWithPackageOrderByCalculatedAtDesc();
        List<CourierPayout> payouts = courierPayouts.findAllWithRelationsOrderByCreatedAtDesc();

        FinancialDashboardDTO dashboard = new FinancialDashboardDTO();
        dashboard.setGeneratedAt(Timestamp.valueOf(LocalDateTime.now()));
        dashboard.setCurrency(resolveDashboardCurrency(settlements, payouts));
        dashboard.setSettlementCount(settlements.size());
        dashboard.setDeliveredPackageCount(settlements.stream()
                .filter(settlement -> settlement.getaPackage() != null && PACKAGE_STATUS.DELIVERED.equals(settlement.getaPackage().getStatus()))
                .count());

        double customerRevenue = settlements.stream().mapToDouble(settlement -> safeDouble(settlement.getCustomerTotalPrice())).sum();
        double serviceFees = settlements.stream().mapToDouble(settlement -> safeDouble(settlement.getPlatformServiceFee())).sum();
        double platformCommission = settlements.stream().mapToDouble(settlement -> safeDouble(settlement.getPlatformCommissionAmount())).sum();
        double platformMargin = serviceFees + platformCommission;
        double averageOrderValue = settlements.isEmpty() ? 0d : customerRevenue / settlements.size();
        double averageCourierPayout = settlements.isEmpty()
                ? 0d
                : settlements.stream().mapToDouble(settlement -> safeDouble(settlement.getCourierPayoutAmount())).sum() / settlements.size();
        double takeRate = customerRevenue <= 0d ? 0d : platformMargin / customerRevenue;

        List<CourierPayout> pendingPayouts = payouts.stream()
                .filter(payout -> payout.getStatus() != null && OPEN_PAYOUT_STATUSES.contains(payout.getStatus()))
                .toList();
        List<CourierPayout> paidPayouts = payouts.stream()
                .filter(payout -> COURIER_PAYOUT_STATUS.PAID.equals(payout.getStatus()))
                .toList();

        dashboard.setCustomerRevenue(customerRevenue);
        dashboard.setPlatformServiceFees(serviceFees);
        dashboard.setPlatformCommissionAmount(platformCommission);
        dashboard.setPlatformMargin(platformMargin);
        dashboard.setAverageOrderValue(averageOrderValue);
        dashboard.setAverageCourierPayout(averageCourierPayout);
        dashboard.setPlatformTakeRate(takeRate);
        dashboard.setPendingPayoutCount(pendingPayouts.size());
        dashboard.setPaidPayoutCount(paidPayouts.size());
        dashboard.setCourierPayoutPendingAmount(pendingPayouts.stream().mapToDouble(payout -> safeDouble(payout.getAmount())).sum());
        dashboard.setCourierPayoutPaidAmount(paidPayouts.stream().mapToDouble(payout -> safeDouble(payout.getAmount())).sum());
        dashboard.setCustomerRevenueTrend(buildSettlementTrend(settlements, PackageSettlement::getCustomerTotalPrice));
        dashboard.setPlatformMarginTrend(buildSettlementTrend(settlements,
                settlement -> safeDouble(settlement.getPlatformServiceFee()) + safeDouble(settlement.getPlatformCommissionAmount())));
        dashboard.setCourierPayoutTrend(buildSettlementTrend(settlements, PackageSettlement::getCourierPayoutAmount));
        dashboard.setRecentSettlements(buildRecentSettlements(settlements));
        dashboard.setPendingPayouts(buildPendingPayoutSummaries(pendingPayouts, dashboard.getCurrency()));
        return dashboard;
    }

    @Override
    public Map<String, PositionDTO> handleWebsocketMessage(MessageDTO messageDTO) {
        if ("PACKAGE_POSITION_UPDATE".equals(messageDTO.getType())
                && messageDTO.getFrom() != null
                && messageDTO.getPositionDTO() != null) {
            return updateTrackingPosition(Long.parseLong(messageDTO.getFrom()), messageDTO.getPositionDTO());
        }
        return Collections.emptyMap();
    }

    @Override
    @Transactional
    public Map<String, PositionDTO> updateTrackingPosition(Long deliveryPersonId, PositionDTO positionDTO) {
        if (deliveryPersonId == null || positionDTO == null
                || positionDTO.getLatitude() == null || positionDTO.getLongitude() == null) {
            return Collections.emptyMap();
        }

        List<String> packageReferences = packages.findPackageReferencesInDeliveryByDeliveryPerson(deliveryPersonId);
        if (packageReferences.isEmpty()) {
            return Collections.emptyMap();
        }

        packages.updateTrackingPositionByDeliveryPerson(
                deliveryPersonId,
                positionDTO.getLatitude(),
                positionDTO.getLongitude()
        );

        Map<String, PositionDTO> updatedPositions = new HashMap<>();
        packageReferences.forEach(packageReference -> updatedPositions.put(packageReference, positionDTO));
        return updatedPositions;
    }

    private List<FinancialTrendPointDTO> buildSettlementTrend(List<PackageSettlement> settlements,
                                                              java.util.function.Function<PackageSettlement, Double> valueExtractor) {
        LinkedHashMap<YearMonth, Double> monthlyValues = new LinkedHashMap<>();
        YearMonth currentMonth = YearMonth.now();
        for (int monthOffset = FINANCIAL_TREND_MONTHS - 1; monthOffset >= 0; monthOffset--) {
            monthlyValues.put(currentMonth.minusMonths(monthOffset), 0d);
        }

        settlements.stream()
                .filter(settlement -> settlement.getCalculatedAt() != null)
                .forEach(settlement -> {
                    YearMonth period = YearMonth.from(settlement.getCalculatedAt().toLocalDateTime());
                    if (monthlyValues.containsKey(period)) {
                        monthlyValues.computeIfPresent(period, (key, value) -> value + safeDouble(valueExtractor.apply(settlement)));
                    }
                });

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        return monthlyValues.entrySet().stream()
                .map(entry -> {
                    FinancialTrendPointDTO point = new FinancialTrendPointDTO();
                    point.setPeriodKey(entry.getKey().format(formatter));
                    point.setValue(roundMoney(entry.getValue()));
                    return point;
                })
                .toList();
    }

    private List<FinancialRecentSettlementDTO> buildRecentSettlements(List<PackageSettlement> settlements) {
        return settlements.stream()
                .limit(FINANCIAL_RECENT_SETTLEMENT_LIMIT)
                .map(settlement -> {
                    FinancialRecentSettlementDTO item = new FinancialRecentSettlementDTO();
                    item.setPackageReference(settlement.getaPackage() == null ? null : settlement.getaPackage().getReference());
                    item.setPackageStatus(settlement.getaPackage() == null || settlement.getaPackage().getStatus() == null
                            ? null
                            : settlement.getaPackage().getStatus().name());
                    item.setCalculatedAt(settlement.getCalculatedAt());
                    item.setCustomerTotalPrice(roundMoney(settlement.getCustomerTotalPrice()));
                    item.setPlatformServiceFee(roundMoney(settlement.getPlatformServiceFee()));
                    item.setPlatformCommissionAmount(roundMoney(settlement.getPlatformCommissionAmount()));
                    item.setCourierPayoutAmount(roundMoney(settlement.getCourierPayoutAmount()));
                    item.setCurrency(settlement.getCurrency());
                    return item;
                })
                .toList();
    }

    private List<CourierPayoutSummaryDTO> buildPendingPayoutSummaries(List<CourierPayout> pendingPayouts, String fallbackCurrency) {
        record CourierAccumulator(String deliveryPersonName, long packageCount, double amount, Timestamp oldestCreatedAt) {}

        Map<Long, CourierAccumulator> grouped = new LinkedHashMap<>();
        pendingPayouts.forEach(payout -> {
            Long courierId = payout.getDeliveryPerson() == null ? -1L : payout.getDeliveryPerson().getId();
            CourierAccumulator current = grouped.get(courierId);
            String courierName = resolveDeliveryPersonName(payout.getDeliveryPerson());
            Timestamp createdAt = payout.getCreatedAt();
            if (current == null) {
                grouped.put(courierId, new CourierAccumulator(courierName, 1L, safeDouble(payout.getAmount()), createdAt));
                return;
            }
            Timestamp oldestCreatedAt = current.oldestCreatedAt();
            if (oldestCreatedAt == null || (createdAt != null && createdAt.before(oldestCreatedAt))) {
                oldestCreatedAt = createdAt;
            }
            grouped.put(courierId, new CourierAccumulator(
                    current.deliveryPersonName(),
                    current.packageCount() + 1,
                    current.amount() + safeDouble(payout.getAmount()),
                    oldestCreatedAt
            ));
        });

        return grouped.entrySet().stream()
                .map(entry -> {
                    CourierPayoutSummaryDTO item = new CourierPayoutSummaryDTO();
                    item.setDeliveryPersonId(entry.getKey() < 0 ? null : entry.getKey());
                    item.setDeliveryPersonName(entry.getValue().deliveryPersonName());
                    item.setPackageCount(entry.getValue().packageCount());
                    item.setAmount(roundMoney(entry.getValue().amount()));
                    item.setOldestCreatedAt(entry.getValue().oldestCreatedAt());
                    item.setCurrency(fallbackCurrency);
                    return item;
                })
                .sorted(Comparator.comparingDouble((CourierPayoutSummaryDTO item) -> safeDouble(item.getAmount())).reversed())
                .limit(FINANCIAL_PENDING_PAYOUT_LIMIT)
                .toList();
    }

    private String resolveDashboardCurrency(List<PackageSettlement> settlements, List<CourierPayout> payouts) {
        return settlements.stream()
                .map(PackageSettlement::getCurrency)
                .filter(Objects::nonNull)
                .filter(currency -> !currency.isBlank())
                .findFirst()
                .orElseGet(() -> payouts.stream()
                        .map(payout -> payout.getaPackage() == null || payout.getaPackage().getPackageSettlement() == null
                                ? null
                                : payout.getaPackage().getPackageSettlement().getCurrency())
                        .filter(Objects::nonNull)
                        .filter(currency -> !currency.isBlank())
                        .findFirst()
                        .orElse("EUR"));
    }

    private String resolveDeliveryPersonName(User deliveryPerson) {
        if (deliveryPerson == null) {
            return "Livreur non assigne";
        }
        String fullName = Stream.of(deliveryPerson.getFirstName(), deliveryPerson.getLastName())
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" "));
        if (!fullName.isBlank()) {
            return fullName;
        }
        return deliveryPerson.getEmailAddress() == null || deliveryPerson.getEmailAddress().isBlank()
                ? "Livreur #" + deliveryPerson.getId()
                : deliveryPerson.getEmailAddress();
    }

    private double safeDouble(Double value) {
        return value == null ? 0d : value;
    }

    private double roundMoney(Double value) {
        return roundMoney(safeDouble(value));
    }

    private double roundMoney(double value) {
        return Math.round(value * 100d) / 100d;
    }

    private void createCourierPayoutIfNeeded(Package aPackage, User deliveryPerson) {
        if (aPackage == null || aPackage.getId() == null || deliveryPerson == null || courierPayouts.existsByaPackageId(aPackage.getId())) {
            return;
        }
        PackageSettlement settlement = aPackage.getPackageSettlement();
        if (settlement == null) {
            return;
        }

        CourierPayout courierPayout = new CourierPayout();
        courierPayout.setaPackage(aPackage);
        courierPayout.setDeliveryPerson(deliveryPerson);
        courierPayout.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        courierPayout.setAmount(settlement.getCourierPayoutAmount());
        courierPayout.setStatus(COURIER_PAYOUT_STATUS.PENDING);
        courierPayouts.save(courierPayout);
    }

    private PackageDTO toPackageDTO(Package aPackage) {
        PackageDTO packageDTO = new PackageDTO();
        packageDTO.setId(aPackage.getId());
        packageDTO.setVersion(aPackage.getVersion());
        packageDTO.setReference(aPackage.getReference());
        packageDTO.setCreationDate(aPackage.getCreationDate());
        packageDTO.setHeight(aPackage.getHeight());
        packageDTO.setWidth(aPackage.getWidth());
        packageDTO.setDepth(aPackage.getDepth());
        packageDTO.setWeight(aPackage.getWeight());
        packageDTO.setPictureURL(aPackage.getPictureURL());
        packageDTO.setStatus(aPackage.getStatus());
        packageDTO.setDeliveryPrice(aPackage.getDeliveryPrice());
        PackageSettlement settlement = aPackage.getPackageSettlement();
        if (settlement != null) {
            packageDTO.setCustomerTotalPrice(settlement.getCustomerTotalPrice());
            packageDTO.setDeliveryBaseAmount(settlement.getDeliveryBaseAmount());
            packageDTO.setInsuranceFee(settlement.getInsuranceFee());
            packageDTO.setPlatformServiceFee(settlement.getPlatformServiceFee());
            packageDTO.setDeliveryRevenueExcludingServiceFee(settlement.getDeliveryRevenueExcludingServiceFee());
            packageDTO.setPlatformCommissionRate(settlement.getPlatformCommissionRate());
            packageDTO.setPlatformCommissionAmount(settlement.getPlatformCommissionAmount());
            packageDTO.setCourierShareRate(settlement.getCourierShareRate());
            packageDTO.setCourierPayoutAmount(settlement.getCourierPayoutAmount());
            packageDTO.setCurrency(settlement.getCurrency());
            packageDTO.setPricingVersion(settlement.getPricingVersion());
            packageDTO.setCalculatedAt(settlement.getCalculatedAt());
        }
        packageDTO.setDeliverySpeed(aPackage.getDeliverySpeed());
        packageDTO.setInsuranceSelected(aPackage.getInsuranceSelected());
        packageDTO.setDeclaredValue(aPackage.getDeclaredValue());
        packageDTO.setDistanceToDestination(aPackage.getDistanceToDestination());
        packageDTO.setGuestMode(aPackage.getGuestMode());
        packageDTO.setGuestAccessToken(aPackage.getGuestAccessToken());
        packageDTO.setLastPositionLatitude(aPackage.getLastPositionLatitude());
        packageDTO.setLastPositionLongitude(aPackage.getLastPositionLongitude());
        packageDTO.setSenderID(aPackage.getSender() == null ? null : aPackage.getSender().getId());
        packageDTO.setAddresses(aPackage.getAddresses() == null ? new ArrayList<>() : aPackage.getAddresses().stream()
                .map(this::toAddressDTO)
                .toList());
        packageDTO.setPackageReservations(aPackage.getPackageReservations() == null ? new ArrayList<>() : aPackage.getPackageReservations().stream()
                .map(this::toPackageReservationDTO)
                .toList());
        packageDTO.setDocumentS(new LinkedHashMap<>());
        aPackage.getDocument().stream()
                .filter(this::isPackageDocument)
                .forEach(document -> packageDTO.getDocumentS().put(document.getType(), toPackageDocumentMetadata(document)));
        return packageDTO;
    }

    private AddressDTO toAddressDTO(Address address) {
        AddressDTO addressDTO = new AddressDTO();
        addressDTO.setId(address.getId());
        addressDTO.setVersion(address.getVersion());
        addressDTO.setFirstName(address.getFirstName());
        addressDTO.setLastName(address.getLastName());
        addressDTO.setLine1(address.getLine1());
        addressDTO.setLine2(address.getLine2());
        addressDTO.setTown(address.getTown());
        addressDTO.setZipCode(address.getZipCode());
        addressDTO.setCountry(address.getCountry());
        addressDTO.setFloor(address.getFloor());
        addressDTO.setHasElevator(address.getHasElevator());
        addressDTO.setDateTime(address.getDateTime());
        addressDTO.setType(address.getType());
        addressDTO.setLatitude(address.getLatitude());
        addressDTO.setLongitude(address.getLongitude());
        addressDTO.setEmail(address.getEmail());
        addressDTO.setPhone(address.getPhone());
        addressDTO.setAddressAuto(addressDTO.toString());
        return addressDTO;
    }

    private PackageReservationDTO toPackageReservationDTO(PackageReservation reservation) {
        PackageReservationDTO reservationDTO = new PackageReservationDTO();
        reservationDTO.setId(reservation.getId());
        reservationDTO.setVersion(reservation.getVersion());
        reservationDTO.setReservationDate(reservation.getReservationDate());
        reservationDTO.setPickUpOTP(reservation.getPickUpOTP());
        reservationDTO.setDeliveryOTP(reservation.getDeliveryOTP());
        reservationDTO.setStatus(reservation.getStatus());
        return reservationDTO;
    }

    private boolean isPackageDocument(Document document) {
        return DOCUMENT_TYPE.PACKAGE_PICTURE.equals(document.getType())
                || DOCUMENT_TYPE.PACKAGE_INVOICE.equals(document.getType());
    }

    private DocumentDTO toPackageDocumentMetadata(Document document) {
        DocumentDTO documentDTO = modelMapper.map(document, DocumentDTO.class);
        documentDTO.setFileName(resolveDocumentFileName(document));
        documentDTO.setData(null);
        return documentDTO;
    }

    private String resolveDocumentFileName(Document document) {
        if (document.getDocURL() == null || document.getDocURL().isBlank()) {
            return document.getType() != null ? document.getType().toString() : null;
        }
        return Paths.get(document.getDocURL()).getFileName().toString();
    }

    private String resolveDocumentContentType(Document document) {
        String fileName = resolveDocumentFileName(document);
        if (fileName == null || !fileName.contains(".")) {
            return DOCUMENT_TYPE.PACKAGE_INVOICE.equals(document.getType()) ? "application/pdf" : "image/png";
        }
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "image/png";
        };
    }

    private PackageReservation reservePackageInternal(Long packageID, Long deliveryPersonID) throws NoSuchAlgorithmException {
        Package aPackage = packages.findById(packageID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
        User user = users.findById(deliveryPersonID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery user not found"));

        if (!PACKAGE_STATUS.NEW.equals(aPackage.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Package is not available for reservation");
        }

        boolean alreadyReservedByUser = aPackage.getPackageReservations() != null && aPackage.getPackageReservations().stream()
                .anyMatch(reservation -> reservation.getDeliveryPerson() != null
                        && Objects.equals(reservation.getDeliveryPerson().getId(), deliveryPersonID)
                        && PACKAGE_RESERVATION_STATUS.ONGOING.equals(reservation.getStatus()));
        if (alreadyReservedByUser) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Package already reserved by this delivery person");
        }

        aPackage.setStatus(PACKAGE_STATUS.RESERVED);
        PackageReservation packageReservation = new PackageReservation();
        packageReservation.setStatus(PACKAGE_RESERVATION_STATUS.ONGOING);
        packageReservation.setaPackage(aPackage);
        packageReservation.setDeliveryPerson(user);
        packageReservation.setReservationDate(Timestamp.valueOf(LocalDateTime.now()));
        packageReservation.setPickUpOTP(OTPHelper.generateOTP(OTPSecret, System.currentTimeMillis()));
        aPackage.getPackageReservations().add(packageReservation);
        packages.saveAndFlush(aPackage);
        return packageReservation;
    }

    private void rollbackFailedPackageCreation(Package aPackage, String packageDirectory) {
        if (aPackage != null && aPackage.getId() != null) {
            try {
                packages.deleteById(aPackage.getId());
                packages.flush();
            } catch (Exception cleanupException) {
                logger.error("Unable to rollback persisted package {} after file failure: {}",
                        aPackage.getReference(), cleanupException.getMessage(), cleanupException);
            }
        }
        if (packageDirectory == null || packageDirectory.isBlank()) {
            return;
        }
        Path directoryPath = Paths.get(packageDirectory);
        if (!Files.exists(directoryPath)) {
            return;
        }
        try (java.util.stream.Stream<Path> pathStream = Files.walk(directoryPath)) {
            pathStream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            logger.warn("Unable to delete {} while rolling back package creation: {}",
                                    path, exception.getMessage());
                        }
                    });
        } catch (IOException exception) {
            logger.warn("Unable to cleanup files at {} after package creation failure: {}",
                    packageDirectory, exception.getMessage());
        }
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadius = 6371000d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2d) * Math.sin(dLat / 2d)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2d) * Math.sin(dLon / 2d);
        return 2d * earthRadius * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
    }

    private static double metersToLatitudeDelta(double meters) {
        return meters / 111320d;
    }

    private static double metersToLongitudeDelta(double meters, double latitude) {
        double cosLat = Math.cos(Math.toRadians(latitude));
        if (Math.abs(cosLat) < 1e-9) {
            return meters / 111320d;
        }
        return meters / (111320d * cosLat);
    }

    private static double pointToSegmentDistanceMeters(GeoPoint point, GeoPoint start, GeoPoint end) {
        double midLat = (start.lat + end.lat) / 2d;
        double metersPerLat = 111320d;
        double metersPerLng = 111320d * Math.cos(Math.toRadians(midLat));

        double sx = start.lng * metersPerLng;
        double sy = start.lat * metersPerLat;
        double ex = end.lng * metersPerLng;
        double ey = end.lat * metersPerLat;
        double px = point.lng * metersPerLng;
        double py = point.lat * metersPerLat;

        double dx = ex - sx;
        double dy = ey - sy;
        double lengthSq = dx * dx + dy * dy;
        if (lengthSq < 1d) {
            return Math.sqrt((px - sx) * (px - sx) + (py - sy) * (py - sy));
        }
        double t = ((px - sx) * dx + (py - sy) * dy) / lengthSq;
        t = Math.max(0d, Math.min(1d, t));
        double cx = sx + t * dx;
        double cy = sy + t * dy;
        return Math.sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy));
    }

    private static double projectionProgress(GeoPoint point, GeoPoint start, GeoPoint end) {
        double midLat = (start.lat + end.lat) / 2d;
        double metersPerLat = 111320d;
        double metersPerLng = 111320d * Math.cos(Math.toRadians(midLat));

        double sx = start.lng * metersPerLng;
        double sy = start.lat * metersPerLat;
        double ex = end.lng * metersPerLng;
        double ey = end.lat * metersPerLat;
        double px = point.lng * metersPerLng;
        double py = point.lat * metersPerLat;

        double dx = ex - sx;
        double dy = ey - sy;
        double lengthSq = dx * dx + dy * dy;
        if (lengthSq < 1d) {
            return 0d;
        }
        return ((px - sx) * dx + (py - sy) * dy) / lengthSq;
    }

    private static final class GeoPoint {
        private final double lat;
        private final double lng;

        private GeoPoint(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }
    }

    private AddressDTO getDepartureAddress(List<AddressDTO> addresses) {
        for (AddressDTO address : addresses) {
            if (ADDRESS_TYPE.DEPARTURE.equals(address.getType())) {
                return address;
            }
        }
        return null;
    }
    private Address getDepartureAddress(Set<Address> addresses) {
        for (Address address : addresses) {
            if (ADDRESS_TYPE.DEPARTURE.equals(address.getType())) {
                return address;
            }
        }
        return null;
    }
    private AddressDTO getArrivalAddress(List<AddressDTO> addresses) {
        for (AddressDTO address : addresses) {
            if (ADDRESS_TYPE.ARRIVAL.equals(address.getType())) {
                return address;
            }
        }
        return null;
    }
    private Address getArrivalAddress(Set<Address> addresses) {
        for (Address address : addresses) {
            if (ADDRESS_TYPE.ARRIVAL.equals(address.getType())) {
                return address;
            }
        }
        return null;
    }

    private <T> T recordPackageOperation(String operation, ThrowingSupplier<T> action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T result = action.get();
            stopPackageOperationTimer(sample, operation, "success");
            incrementPackageOperationCounter(operation, "success");
            return result;
        } catch (RuntimeException exception) {
            stopPackageOperationTimer(sample, operation, "failure");
            incrementPackageOperationCounter(operation, "failure");
            throw exception;
        } catch (Exception exception) {
            stopPackageOperationTimer(sample, operation, "failure");
            incrementPackageOperationCounter(operation, "failure");
            throw new RuntimeException(exception);
        }
    }

    private <T> T recordPackageOperationChecked(String operation, ThrowingSupplier<T> action) throws NoSuchAlgorithmException {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T result = action.get();
            stopPackageOperationTimer(sample, operation, "success");
            incrementPackageOperationCounter(operation, "success");
            return result;
        } catch (NoSuchAlgorithmException exception) {
            stopPackageOperationTimer(sample, operation, "failure");
            incrementPackageOperationCounter(operation, "failure");
            throw exception;
        } catch (RuntimeException exception) {
            stopPackageOperationTimer(sample, operation, "failure");
            incrementPackageOperationCounter(operation, "failure");
            throw exception;
        } catch (Exception exception) {
            stopPackageOperationTimer(sample, operation, "failure");
            incrementPackageOperationCounter(operation, "failure");
            throw new RuntimeException(exception);
        }
    }

    private void stopPackageOperationTimer(Timer.Sample sample, String operation, String result) {
        sample.stop(Timer.builder("quickdelivery.packages.operation.duration")
                .tag("operation", operation)
                .tag("result", result)
                .register(meterRegistry));
    }

    private void incrementPackageOperationCounter(String operation, String result) {
        meterRegistry.counter("quickdelivery.packages.operation.calls",
                "operation", operation,
                "result", result).increment();
    }

    private void recordPackageDocumentContentSize(int sizeInBytes) {
        DistributionSummary.builder("quickdelivery.packages.document.content.bytes")
                .baseUnit("bytes")
                .register(meterRegistry)
                .record(sizeInBytes);
    }

    private Double readGauge(String metricName, String... tags) {
        try {
            var search = meterRegistry.find(metricName);
            for (int index = 0; index + 1 < tags.length; index += 2) {
                search = search.tag(tags[index], tags[index + 1]);
            }
            if (search.gauge() != null) {
                return search.gauge().value();
            }
        } catch (Exception ignored) {
            // Ignore unavailable runtime metrics.
        }
        return null;
    }

    private Double sumMetric(String metricName) {
        try {
            return meterRegistry.find(metricName)
                    .meters()
                    .stream()
                    .flatMap(meter -> StreamSupport.stream(meter.measure().spliterator(), false))
                    .mapToDouble(measurement -> ((Measurement) measurement).getValue())
                    .sum();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void collectHttpEndpointMetric(Map<String, HttpEndpointMetricDTO> aggregated, Meter meter) {
        String uri = meter.getId().getTag("uri");
        if (shouldIgnoreHttpUri(uri)) {
            return;
        }
        String method = Optional.ofNullable(meter.getId().getTag("method")).orElse("GET");
        double count = readStatistic(meter, Statistic.COUNT);
        if (count <= 0d) {
            return;
        }
        double maxResponseTimeMs = readStatistic(meter, Statistic.MAX) * 1000d;
        String key = method + " " + uri;
        HttpEndpointMetricDTO endpointMetric = aggregated.computeIfAbsent(key, ignored -> {
            HttpEndpointMetricDTO metric = new HttpEndpointMetricDTO();
            metric.setMethod(method);
            metric.setEndpoint(uri);
            return metric;
        });
        endpointMetric.setRequestCount(endpointMetric.getRequestCount() + count);
        endpointMetric.setMaxResponseTimeMs(Math.max(endpointMetric.getMaxResponseTimeMs(), maxResponseTimeMs));
    }

    private double readStatistic(Meter meter, Statistic statistic) {
        return StreamSupport.stream(meter.measure().spliterator(), false)
                .filter(measurement -> measurement.getStatistic() == statistic)
                .mapToDouble(Measurement::getValue)
                .findFirst()
                .orElse(0d);
    }

    private boolean shouldIgnoreHttpUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return true;
        }
        String normalizedUri = uri.toLowerCase(Locale.ROOT);
        return normalizedUri.startsWith("/actuator")
                || normalizedUri.startsWith("/packages/v1/admin")
                || normalizedUri.contains("/admin/http-breakdown")
                || normalizedUri.contains("/admin/metrics")
                || normalizedUri.contains("/admin/log-insights")
                || "unknown".equals(normalizedUri)
                || "/error".equals(normalizedUri);
    }

    private Double toMegabytes(Double valueInBytes) {
        return valueInBytes == null ? null : valueInBytes / (1024d * 1024d);
    }

    private Double toPercent(Double ratio) {
        return ratio == null ? null : ratio * 100d;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
