package com.quickdelivery.services.implementations;

import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.errors.RequestDeniedException;
import com.google.maps.model.DistanceMatrix;
import com.quickdelivery.PublicFrontendUrlResolver;
import com.quickdelivery.abstarct.dto.*;
import com.quickdelivery.abstarct.entities.*;
import com.quickdelivery.abstarct.entities.Package;
import com.quickdelivery.abstarct.helpers.GeoHelper;
import com.quickdelivery.abstarct.helpers.MailHelper;
import com.quickdelivery.abstarct.helpers.OTPHelper;
import com.quickdelivery.abstarct.helpers.PDFGenerator;
import com.quickdelivery.abstarct.helpers.QRCodeGenerator;
import com.quickdelivery.abstarct.parameters.*;
import com.quickdelivery.abstarct.repositories.CourierPayouts;
import com.quickdelivery.abstarct.repositories.CourierPenalties;
import com.quickdelivery.abstarct.repositories.DeliveryRoutes;
import com.quickdelivery.abstarct.repositories.DeliveryRouteStops;
import com.quickdelivery.abstarct.repositories.Documents;
import com.quickdelivery.abstarct.repositories.PackageReservations;
import com.quickdelivery.abstarct.repositories.PackageSettlements;
import com.quickdelivery.abstarct.repositories.Packages;
import com.quickdelivery.abstarct.repositories.Users;
import com.quickdelivery.dto.ReserveBatchPlanRequestDTO;
import com.quickdelivery.dto.ReserveBatchResultDTO;
import com.quickdelivery.dto.ReservationAvailabilityDTO;
import com.quickdelivery.dto.RoutePlanDTO;
import com.quickdelivery.dto.RoutePlanMetricsDTO;
import com.quickdelivery.dto.RoutePlanPackageAnnotationDTO;
import com.quickdelivery.dto.RoutePlanPointDTO;
import com.quickdelivery.dto.RoutePlanRequestDTO;
import com.quickdelivery.dto.RoutePlanStopDTO;
import com.quickdelivery.config.TrackingBroadcastPublisher;
import com.quickdelivery.config.TrackingPositionCacheService;
import com.quickdelivery.helpers.PackageDeliveryPriceCalculator;
import com.quickdelivery.helpers.PackagePricingBreakdown;
import com.quickdelivery.services.interfaces.INotificationService;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
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
    @Value("${quickdelivery.notifications.email.enabled:true}")
    private boolean emailNotificationsEnabled;
    @Value("${quickdelivery.notifications.email.cooldown-seconds:300}")
    private long emailNotificationCooldownSeconds;
    @Value("${quickdelivery.packages.nearby.max-results:150}")
    private int maxNearbyResults;
    @Value("${quickdelivery.packages.status.max-results:200}")
    private int maxPackagesByStatusResults;
    @Value("${quickdelivery.google-maps.distance-provider-cooldown-seconds:900}")
    private long googleMapsDistanceProviderCooldownSeconds;
    @Value("${quickdelivery.packages.distance-cache.ttl-seconds:600}")
    private long distanceCacheTtlSeconds;
    @Value("${quickdelivery.packages.distance-cache.max-entries:5000}")
    private int maxDistanceCacheEntries;
    @Value("${quickdelivery.routes.stop-validation-radius-meters:150}")
    private double stopValidationRadiusMeters;
    @Value("${quickdelivery.routes.start-deadline-minutes:30}")
    private long routeStartDeadlineMinutes;
    @Value("${quickdelivery.routes.no-progress-deadline-minutes:30}")
    private long routeNoProgressDeadlineMinutes;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private Packages packages;
    @Autowired
    private Documents documents;
    @Autowired
    private DeliveryRoutes deliveryRoutes;
    @Autowired
    private DeliveryRouteStops deliveryRouteStops;
    @Autowired
    private CourierPayouts courierPayouts;
    @Autowired
    private CourierPenalties courierPenalties;
    @Autowired
    private PackageReservations packageReservations;
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
    @Autowired
    private PackageDocumentStorageService packageDocumentStorageService;
    @Autowired
    @Lazy
    private TrackingBroadcastPublisher trackingBroadcastPublisher;
    @Autowired
    private TrackingPositionCacheService trackingPositionCacheService;
    @Autowired
    private INotificationService notificationService;
    private final AtomicLong emailNotificationsDisabledUntilEpochMs = new AtomicLong(0L);
    private final AtomicLong googleMapsDistanceProviderDisabledUntilEpochMs = new AtomicLong(0L);
    private final ConcurrentHashMap<String, CachedRouteDistance> distanceCache = new ConcurrentHashMap<>();
    private static final int FINANCIAL_TREND_MONTHS = 6;
    private static final int FINANCIAL_RECENT_SETTLEMENT_LIMIT = 8;
    private static final int FINANCIAL_PENDING_PAYOUT_LIMIT = 8;
    private static final Set<COURIER_PAYOUT_STATUS> OPEN_PAYOUT_STATUSES = EnumSet.of(
            COURIER_PAYOUT_STATUS.PENDING,
            COURIER_PAYOUT_STATUS.APPROVED,
            COURIER_PAYOUT_STATUS.FAILED
    );
    private static final EnumSet<DELIVERY_ROUTE_STATUS> ACTIVE_DELIVERY_ROUTE_STATUSES = EnumSet.of(
            DELIVERY_ROUTE_STATUS.PLANNED,
            DELIVERY_ROUTE_STATUS.ACTIVE
    );

    @Scheduled(fixedDelayString = "${quickdelivery.routes.penalty-scan-ms:60000}")
    @Transactional
    public void scanRoutePenaltyDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        if (routeStartDeadlineMinutes > 0) {
            Timestamp plannedCutoff = Timestamp.valueOf(now.minusMinutes(routeStartDeadlineMinutes));
            deliveryRoutes.findByStatusAndCreatedBefore(DELIVERY_ROUTE_STATUS.PLANNED, plannedCutoff)
                    .forEach(route -> persistCourierPenalty(
                            route.getDeliveryPerson(),
                            route,
                            null,
                            COURIER_PENALTY_TYPE.ROUTE_NOT_STARTED_DEADLINE,
                            2,
                            "routeNotStartedBeforeDeadline",
                            "{\"deliveryRouteId\":" + route.getId() + ",\"deadlineMinutes\":" + routeStartDeadlineMinutes + "}",
                            false
                    ));
        }
        if (routeNoProgressDeadlineMinutes > 0) {
            Timestamp activeCutoff = Timestamp.valueOf(now.minusMinutes(routeNoProgressDeadlineMinutes));
            deliveryRoutes.findActiveRoutesWithoutCompletedStop(
                            DELIVERY_ROUTE_STATUS.ACTIVE,
                            activeCutoff,
                            DELIVERY_ROUTE_STOP_STATUS.DONE
                    )
                    .forEach(route -> persistCourierPenalty(
                            route.getDeliveryPerson(),
                            route,
                            null,
                            COURIER_PENALTY_TYPE.ROUTE_STARTED_NO_PROGRESS,
                            3,
                            "routeStartedWithoutStopProgress",
                            "{\"deliveryRouteId\":" + route.getId() + ",\"deadlineMinutes\":" + routeNoProgressDeadlineMinutes + "}",
                            false
                    ));
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "packagesByStatus", allEntries = true),
            @CacheEvict(value = "packagesAroundGrouped", allEntries = true),
            @CacheEvict(value = "packagesAroundMe", allEntries = true),
            @CacheEvict(value = "packagesAroundDestination", allEntries = true),
            @CacheEvict(value = "packagesByDeliveryPerson", allEntries = true),
            @CacheEvict(value = "packagesBySender", allEntries = true),
            @CacheEvict(value = "userWithOngoingDelivery", allEntries = true),
            @CacheEvict(value = "packagesAdminDashboardSummary", allEntries = true),
            @CacheEvict(value = "packagesAdminFinancialDashboard", allEntries = true)
    })
    public PackageDTO createNewPackage(PackageDTO packageDTO, MultipartFile[] files, Locale locale) {
        return recordPackageOperation("create", () -> {
            MultiValueMap<String, MultipartFile> filesMap = new LinkedMultiValueMap<>();
            Package aPackage;
            try {
                validatePackageCreationRequest(packageDTO);
                packageAddressGeocoding(packageDTO);
                double distanceInKilometers = resolveDistanceInKilometers(packageDTO);
                PackagePricingBreakdown pricingBreakdown = calculatePricingBreakdown(packageDTO, distanceInKilometers);
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
                        packageDocumentStorageService.saveFiles(filesMap, packageDirectory, false);
                    }
                } catch (Exception fileFailure) {
                    rollbackFailedPackageCreation(aPackage, packageDirectory);
                    throw fileFailure;
                }
                submitPackageAsyncTask(() -> {
                    try {
                        generatePackageArtifacts(packageDTO, locale);
                        runPackageMailInline(() -> sendPackageCreationEMail(
                                aPackage,
                                locale,
                                EMAIL_TEMPLATE_TYPE.PACKAGE_CREATION.getType(),
                                messageSource.getMessage("email.subject.newPackage", null, locale),
                                EMAIL_TYPE.PACKAGE_CREATION
                        ));
                    } catch (Exception e) {
                        logger.error("Unable to generate package artifacts and send creation email for {}", packageDTO.getReference(), e);
                    }
                }, packageAsyncTaskExecutor, "package-artifacts");
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
            double distanceInKilometers = resolveDistanceInKilometers(packageDTO);
            applyPricing(packageDTO, calculatePricingBreakdown(packageDTO, distanceInKilometers));
            return packageDTO;
        });
    }

    private void generatePackageArtifacts(PackageDTO packageDTO, Locale locale) throws IOException {
        String packageBasePath = packagesDirectory + packageDTO.getReference();
        String qrPath = packageBasePath + packageQrEnds;
        String labelPath = packageBasePath + packageLabelEnds;
        Path tempDir = Files.createTempDirectory("qd-package-artifacts-");
        Path qrFile = tempDir.resolve("package_qr.png");
        Path tempLabelFile = tempDir.resolve("package_label.pdf");
        QRCodeGenerator.generateQRCode(buildPackageConsultationLink(packageDTO.getReference()), qrFile.toString(), 150, 150);
        if (!Files.exists(qrFile) || Files.size(qrFile) == 0) {
            throw new IOException("QR code generation failed for " + packageDTO.getReference());
        }
        PDFGenerator.generatePdf(packageDTO, qrFile.toString(), tempLabelFile.toString(), locale);
        if (!Files.exists(tempLabelFile) || Files.size(tempLabelFile) == 0) {
            throw new IOException("PDF label generation failed for " + packageDTO.getReference());
        }
        packageDocumentStorageService.writeBytes(qrPath, Files.readAllBytes(qrFile), "image/png");
        packageDocumentStorageService.writeBytes(labelPath, Files.readAllBytes(tempLabelFile), "application/pdf");
        cleanupTempArtifacts(tempDir);
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
                double distanceInKilometers = resolveDistanceInKilometers(packageDTO);
                preparPackage(packageDTO, locale, calculatePricingBreakdown(packageDTO, distanceInKilometers));
            } catch (MalformedURLException | FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "packagesAroundGrouped", key = "T(com.quickdelivery.services.implementations.PackagesService).buildNearbyCacheKey(#latitude, #longitude, #rayonEnMetres)", sync = true)
    public Map<String, List<PackageDTO>> getPAckagesAroundPosition(String latitude, String longitude, double rayonEnMetres) {
        return getPAckagesAroundPosition(latitude, longitude, rayonEnMetres, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<PackageDTO>> getPAckagesAroundPosition(String latitude, String longitude, double rayonEnMetres, String deliveryMode) {
        double centerLat = Double.parseDouble(latitude);
        double centerLng = Double.parseDouble(longitude);
        List<PackageDTO> packageDTOS = loadNearbyNewPackages(centerLat, centerLng, rayonEnMetres, deliveryMode);
        return packageDTOS.parallelStream()
                .collect(Collectors.groupingByConcurrent(packaged -> {
                    AddressDTO departureAddress = getDepartureAddress(packaged.getAddresses());
                    double distanceMeters = haversineMeters(
                            departureAddress.getLatitude().doubleValue(),
                            departureAddress.getLongitude().doubleValue(),
                            centerLat,
                            centerLng
                    );
                    return departureAddress.getLatitude()+","+departureAddress.getLongitude()+","+departureAddress.toString()
                            +" ("+formatApproximateDistance(distanceMeters)+")";
                }));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<PackageDTO>> getPAckagesAroundPosition(AddressDTO address, double rayonEnMetres) throws IOException, InterruptedException, ApiException {
        GeoHelper.AddressGeoCoding(geoApiContext,address);
        return getPAckagesAroundPosition(address.getLatitude().toString(), address.getLongitude().toString(), rayonEnMetres);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "packagesAroundMe", key = "T(com.quickdelivery.services.implementations.PackagesService).buildNearbyCacheKey(#latitude, #longitude, #rayonEnMetres)", sync = true)
    public List<PackageDTO> getPackagesAroundPosition(String latitude, String longitude, double rayonEnMetres) {
        return getPackagesAroundPosition(latitude, longitude, rayonEnMetres, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageDTO> getPackagesAroundPosition(String latitude, String longitude, double rayonEnMetres, String deliveryMode) {
        return loadNearbyNewPackages(Double.parseDouble(latitude), Double.parseDouble(longitude), rayonEnMetres, deliveryMode);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "packagesAroundMe", key = "T(java.lang.String).format('bounds:%s:%s:%s:%s:%s:%s:%s', T(com.quickdelivery.services.implementations.PackagesService).normalizeNearbyCoordinateKey(T(java.lang.String).valueOf(#minLat)), T(com.quickdelivery.services.implementations.PackagesService).normalizeNearbyCoordinateKey(T(java.lang.String).valueOf(#maxLat)), T(com.quickdelivery.services.implementations.PackagesService).normalizeNearbyCoordinateKey(T(java.lang.String).valueOf(#minLng)), T(com.quickdelivery.services.implementations.PackagesService).normalizeNearbyCoordinateKey(T(java.lang.String).valueOf(#maxLng)), T(com.quickdelivery.services.implementations.PackagesService).normalizeNearbyCoordinateKey(T(java.lang.String).valueOf(#centerLat)), T(com.quickdelivery.services.implementations.PackagesService).normalizeNearbyCoordinateKey(T(java.lang.String).valueOf(#centerLng)), #limit)", sync = true)
    public List<PackageDTO> getPackagesInBounds(double minLat, double maxLat, double minLng, double maxLng, double centerLat, double centerLng, int limit) {
        return getPackagesInBounds(minLat, maxLat, minLng, maxLng, centerLat, centerLng, limit, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageDTO> getPackagesInBounds(double minLat, double maxLat, double minLng, double maxLng, double centerLat, double centerLng, int limit, String deliveryMode) {
        int effectiveLimit = Math.max(1, Math.min(limit, maxNearbyResults));
        double diagonalMeters = haversineMeters(minLat, minLng, maxLat, maxLng);
        List<Long> nearbyPackageIds = packages.findNearbyNewPackageIds(
                centerLat,
                centerLng,
                Math.max(500d, diagonalMeters / 2d),
                minLat,
                maxLat,
                minLng,
                maxLng,
                effectiveLimit
        );
        return loadNearbyPackagesByIds(centerLat, centerLng, Math.max(500d, diagonalMeters / 2d), nearbyPackageIds, deliveryMode);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "packagesAroundDestination", key = "T(java.lang.String).format('%s:%s:%s:%s:%s:%s:%s', #latitude, #longitude, #destinationAddress.line1, #destinationAddress.zipCode, #destinationAddress.town, #destinationAddress.country, #rayonEnMetres)", sync = true)
    public List<PackageDTO> getPackagesAroundPositionWithDestination(String latitude, String longitude, AddressDTO destinationAddress, double rayonEnMetres) throws IOException, InterruptedException, ApiException {
        return getPackagesAroundPositionWithDestination(latitude, longitude, destinationAddress, rayonEnMetres, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageDTO> getPackagesAroundPositionWithDestination(String latitude, String longitude, AddressDTO destinationAddress, double rayonEnMetres, String deliveryMode) throws IOException, InterruptedException, ApiException {
        return getPackagesAroundPositionWithDestination(latitude, longitude, destinationAddress, rayonEnMetres, deliveryMode, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageDTO> getPackagesAroundPositionWithDestination(String latitude, String longitude, AddressDTO destinationAddress, double rayonEnMetres, String deliveryMode, String vehicleType) throws IOException, InterruptedException, ApiException {
        if (destinationAddress.getLatitude() == null || destinationAddress.getLongitude() == null) {
            GeoHelper.AddressGeoCoding(geoApiContext, destinationAddress);
        }
        if (destinationAddress.getLatitude() == null || destinationAddress.getLongitude() == null) {
            return Collections.emptyList();
        }

        int maxVisiblePackages = resolveMaxPackagesForSearch(deliveryMode, vehicleType);
        return getPackagesAroundPosition(latitude, longitude, rayonEnMetres, deliveryMode).stream()
                .filter(packageDTO -> isPackageCompatibleForSearch(modelMapper.map(packageDTO, Package.class), deliveryMode, vehicleType))
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
                .limit(maxVisiblePackages)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageDTO> findAddressOnMyRoad(String departureLatitude, String arrivalLatitude, String departureLongitude, String arrivalLongitude) {
        return findAddressOnMyRoad(departureLatitude, arrivalLatitude, departureLongitude, arrivalLongitude, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageDTO> findAddressOnMyRoad(String departureLatitude, String arrivalLatitude, String departureLongitude, String arrivalLongitude, String deliveryMode) {
        return findAddressOnMyRoad(departureLatitude, arrivalLatitude, departureLongitude, arrivalLongitude, deliveryMode, null, resolveSearchRadiusForMode(deliveryMode));
    }

    @Override
    @Transactional(readOnly = true)
    public RoutePlanDTO buildRoutePlan(RoutePlanRequestDTO request) {
        if (request == null || request.getStart() == null || request.getEnd() == null || request.getPackageIds() == null || request.getPackageIds().isEmpty()) {
            return null;
        }
        GeoPoint start = toGeoPoint(request.getStart());
        GeoPoint end = toGeoPoint(request.getEnd());
        if (start == null || end == null) {
            return null;
        }

        List<Long> requestedPackageIds = request.getPackageIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (requestedPackageIds.isEmpty()) {
            return null;
        }

        Map<Long, Integer> inputOrder = new HashMap<>();
        for (int index = 0; index < requestedPackageIds.size(); index++) {
            inputOrder.put(requestedPackageIds.get(index), index);
        }

        int maxVisiblePackages = resolveMaxPackagesForSearch(request.getDeliveryMode(), request.getVehicleType());
        List<PlannablePackage> plannablePackages = packages.findPackagesWithAddressesByIds(requestedPackageIds).stream()
                .map(this::normalizeSoftLockState)
                .filter(Objects::nonNull)
                .filter(pkg -> PACKAGE_STATUS.NEW.equals(pkg.getStatus()))
                .filter(pkg -> inputOrder.containsKey(pkg.getId()))
                .filter(pkg -> pkg.getAddresses() != null && pkg.getAddresses().size() >= 2)
                .filter(pkg -> isPackageCompatibleForSearch(pkg, request.getDeliveryMode(), request.getVehicleType()))
                .map(this::toPlannablePackage)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(pkg -> inputOrder.getOrDefault(pkg.packageId(), Integer.MAX_VALUE)))
                .limit(Math.max(1, maxVisiblePackages))
                .collect(Collectors.toList());

        if (plannablePackages.isEmpty()) {
            return null;
        }

        String normalizedMode = normalizeRoutePlanMode(request.getMode());
        List<PlannablePackage> constrainedPackages = "directAddress".equals(normalizedMode)
                ? constrainPackagesForDirectRoute(plannablePackages, start, end, request)
                : constrainPackagesForPersonalRoute(plannablePackages, start, end, request);
        if (constrainedPackages.isEmpty()) {
            return null;
        }
        return "directAddress".equals(normalizedMode)
                ? buildDirectRoutePlanDto(constrainedPackages, start, end, request)
                : buildPersonalRoutePlanDto(constrainedPackages, start, end, request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageDTO> findAddressOnMyRoad(String departureLatitude,
                                                String arrivalLatitude,
                                                String departureLongitude,
                                                String arrivalLongitude,
                                                String deliveryMode,
                                                String vehicleType,
                                                double radiusMeters) {
        double startLat = Double.parseDouble(departureLatitude);
        double startLng = Double.parseDouble(departureLongitude);
        double endLat = Double.parseDouble(arrivalLatitude);
        double endLng = Double.parseDouble(arrivalLongitude);

        double effectiveRadiusMeters = radiusMeters > 0d ? radiusMeters : resolveSearchRadiusForMode(deliveryMode);
        double routeDistanceMeters = haversineMeters(startLat, startLng, endLat, endLng);
        if (routeDistanceMeters < 50d) {
            return Collections.emptyList();
        }

        double corridorMeters = resolveCorridorWidth(deliveryMode, routeDistanceMeters);
        double searchMarginMeters = Math.max(corridorMeters, effectiveRadiusMeters);
        double corridorLatMargin = metersToLatitudeDelta(searchMarginMeters);
        double corridorLngMargin = metersToLongitudeDelta(searchMarginMeters, (startLat + endLat) / 2d);

        double minLat = Math.min(startLat, endLat) - corridorLatMargin;
        double maxLat = Math.max(startLat, endLat) + corridorLatMargin;
        double minLng = Math.min(startLng, endLng) - corridorLngMargin;
        double maxLng = Math.max(startLng, endLng) + corridorLngMargin;

        List<Package> candidatePackages = this.packages.findNewPackagesInBoundingBox(minLat, maxLat, minLng, maxLng);
        GeoPoint routeStart = new GeoPoint(startLat, startLng);
        GeoPoint routeEnd = new GeoPoint(endLat, endLng);
        int maxVisiblePackages = resolveMaxPackagesForSearch(deliveryMode, vehicleType);

        return candidatePackages.stream()
                .map(this::normalizeSoftLockState)
                .filter(Objects::nonNull)
                .filter(pkg -> PACKAGE_STATUS.NEW.equals(pkg.getStatus()))
                .filter(pkg -> pkg.getAddresses() != null && pkg.getAddresses().size() >= 2)
                .map(pkg -> {
                    Address dep = getDepartureAddress(pkg.getAddresses());
                    Address arr = getArrivalAddress(pkg.getAddresses());
                    if (dep == null || arr == null || dep.getLatitude() == null || dep.getLongitude() == null
                            || arr.getLatitude() == null || arr.getLongitude() == null) {
                        return null;
                    }

                    if (!isPackageCompatibleForSearch(pkg, deliveryMode, vehicleType)) {
                        return null;
                    }

                    GeoPoint depPoint = new GeoPoint(dep.getLatitude().doubleValue(), dep.getLongitude().doubleValue());
                    GeoPoint arrPoint = new GeoPoint(arr.getLatitude().doubleValue(), arr.getLongitude().doubleValue());

                    double depDistanceToStart = haversineMeters(depPoint.lat, depPoint.lng, routeStart.lat, routeStart.lng);
                    double arrDistanceToDestination = haversineMeters(arrPoint.lat, arrPoint.lng, routeEnd.lat, routeEnd.lng);
                    double depDistanceToRoute = pointToSegmentDistanceMeters(depPoint, routeStart, routeEnd);
                    double arrDistanceToRoute = pointToSegmentDistanceMeters(arrPoint, routeStart, routeEnd);
                    double depProgress = projectionProgress(depPoint, routeStart, routeEnd);
                    double arrProgress = projectionProgress(arrPoint, routeStart, routeEnd);

                    boolean pickupWithinCourierRadius = depDistanceToStart <= effectiveRadiusMeters;
                    boolean deliveryWithinDestinationRadius = arrDistanceToDestination <= effectiveRadiusMeters;
                    boolean pickupOnPath = isOnPathCandidate(depDistanceToRoute, depProgress, corridorMeters);
                    boolean deliveryOnPath = isOnPathCandidate(arrDistanceToRoute, arrProgress, corridorMeters);
                    double pickupDetourMinutes = convertDetourMetersToMinutes(depDistanceToRoute, deliveryMode);
                    double deliveryDetourMinutes = convertDetourMetersToMinutes(arrDistanceToRoute, deliveryMode);
                    boolean pickupOnPathWithAcceptedDetour = pickupOnPath && pickupDetourMinutes <= 20d;
                    boolean deliveryOnPathWithAcceptedDetour = deliveryOnPath && deliveryDetourMinutes <= 20d;

                    boolean matchesSearch =
                            (pickupWithinCourierRadius && deliveryWithinDestinationRadius)
                                    || (pickupWithinCourierRadius && deliveryOnPathWithAcceptedDetour)
                                    || (pickupOnPathWithAcceptedDetour && deliveryWithinDestinationRadius)
                                    || (pickupOnPathWithAcceptedDetour && deliveryOnPathWithAcceptedDetour && depProgress <= arrProgress);
                    if (!matchesSearch) {
                        return null;
                    }

                    PackageDTO dto = toPackageDTO(pkg);
                    double detourMeters = 0d;
                    if (pickupOnPathWithAcceptedDetour) {
                        detourMeters += depDistanceToRoute;
                    }
                    if (deliveryOnPathWithAcceptedDetour) {
                        detourMeters += arrDistanceToRoute;
                    }
                    dto.setDetourMeters(detourMeters);
                    dto.setProfitScore(null);
                    return dto;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(PackageDTO::getDetourMeters, Comparator.nullsLast(Double::compareTo))
                        .thenComparing(PackageDTO::getId, Comparator.nullsLast(Long::compareTo)))
                .limit(Math.max(1, maxVisiblePackages))
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "packagesByStatus", allEntries = true),
            @CacheEvict(value = "packagesAroundGrouped", allEntries = true),
            @CacheEvict(value = "packagesAroundMe", allEntries = true),
            @CacheEvict(value = "packagesAroundDestination", allEntries = true),
            @CacheEvict(value = "packagesByDeliveryPerson", allEntries = true),
            @CacheEvict(value = "packagesBySender", allEntries = true),
            @CacheEvict(value = "userWithOngoingDelivery", allEntries = true),
            @CacheEvict(value = "packagesAdminDashboardSummary", allEntries = true),
            @CacheEvict(value = "packagesAdminFinancialDashboard", allEntries = true)
    })
    public PackageReservation reservePackage(Long packageID, Long deliveryPersonID, Locale locale) throws NoSuchAlgorithmException {
        return recordPackageOperationChecked("reserve", () -> {
            PackageReservation packageReservation = reservePackageInternal(packageID, deliveryPersonID);
            Package aPackage = packageReservation.getaPackage();
            packages.saveAndFlush(aPackage);
            if (aPackage.getSender() != null && aPackage.getSender().getId() != null) {
                // Notifier l'expéditeur de la réservation avec le code de collecte
                notificationService.createAndDispatch(aPackage.getSender().getId(), NOTIFICATION_EVENT_TYPE.PACKAGE_RESERVED, "/package?id=" + aPackage.getReference(), "{\"packageReference\":\"" + aPackage.getReference() + "\",\"otp\":\"" + packageReservation.getPickUpOTP() + "\"}", packageReservation.getPickUpOTP());
            }
            if (packageReservation.getDeliveryPerson() != null && packageReservation.getDeliveryPerson().getId() != null) {
                notificationService.createAndDispatch(packageReservation.getDeliveryPerson().getId(), NOTIFICATION_EVENT_TYPE.PACKAGE_RESERVATION_OTP, "/package?id=" + aPackage.getReference(), "{\"packageReference\":\"" + aPackage.getReference() + "\",\"otp\":\"" + packageReservation.getPickUpOTP() + "\"}", aPackage.getReference());
            }
            primePackageForAsyncNotifications(aPackage);
            runPackageMailTask(() -> {
                sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_RESERVATION_SENDER.getType(), messageSource.getMessage("email.subject.packageReserved", null, locale),EMAIL_TYPE.PACKAGE_RESERVATION_SENDER);
                sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_RESERVATION_DELIVERY.getType(), messageSource.getMessage("email.subject.packageReservationCofirm", null, locale),EMAIL_TYPE.PACKAGE_RESERVATION_DELIVERY);
            });
            return packageReservation;
        });
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "packagesByStatus", allEntries = true),
            @CacheEvict(value = "packagesAroundGrouped", allEntries = true),
            @CacheEvict(value = "packagesAroundMe", allEntries = true),
            @CacheEvict(value = "packagesAroundDestination", allEntries = true),
            @CacheEvict(value = "packagesByDeliveryPerson", allEntries = true),
            @CacheEvict(value = "packagesBySender", allEntries = true),
            @CacheEvict(value = "userWithOngoingDelivery", allEntries = true),
            @CacheEvict(value = "packagesAdminDashboardSummary", allEntries = true),
            @CacheEvict(value = "packagesAdminFinancialDashboard", allEntries = true)
    })
    @Transactional
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
                    if (aPackage.getSender() != null && aPackage.getSender().getId() != null) {
                        notificationService.createAndDispatch(aPackage.getSender().getId(), NOTIFICATION_EVENT_TYPE.PACKAGE_RESERVED, "/package?id=" + aPackage.getReference(), "{\"packageReference\":\"" + aPackage.getReference() + "\"}");
                    }
                    primePackageForAsyncNotifications(aPackage);
                    runPackageMailTask(() -> {
                        sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_RESERVATION_SENDER.getType(),
                                messageSource.getMessage("email.subject.packageReserved", null, locale), EMAIL_TYPE.PACKAGE_RESERVATION_SENDER);
                        sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_RESERVATION_DELIVERY.getType(),
                                messageSource.getMessage("email.subject.packageReservationCofirm", null, locale), EMAIL_TYPE.PACKAGE_RESERVATION_DELIVERY);
                    });
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
    @Caching(evict = {
            @CacheEvict(value = "packagesByStatus", allEntries = true),
            @CacheEvict(value = "packagesAroundGrouped", allEntries = true),
            @CacheEvict(value = "packagesAroundMe", allEntries = true),
            @CacheEvict(value = "packagesAroundDestination", allEntries = true),
            @CacheEvict(value = "packagesByDeliveryPerson", allEntries = true),
            @CacheEvict(value = "packagesBySender", allEntries = true),
            @CacheEvict(value = "userWithOngoingDelivery", allEntries = true),
            @CacheEvict(value = "packagesAdminDashboardSummary", allEntries = true),
            @CacheEvict(value = "packagesAdminFinancialDashboard", allEntries = true)
    })
    @Transactional
    public ReserveBatchResultDTO reservePackagesBatchFromPlan(ReserveBatchPlanRequestDTO request, Long deliveryPersonID, Locale locale) {
        return recordPackageOperation("reserveBatchPlanned", () -> {
            ReserveBatchResultDTO result = new ReserveBatchResultDTO();
            result.setDeliveryPersonId(deliveryPersonID);
            if (request == null || request.getRoutePlan() == null) {
                result.setRequestedCount(0);
                result.setReservedCount(0);
                return result;
            }

            RoutePlanDTO authoritativePlan = buildRoutePlan(request.getRoutePlan());
            if (authoritativePlan == null || authoritativePlan.getPackageIds() == null || authoritativePlan.getPackageIds().isEmpty()) {
                result.setRequestedCount(0);
                result.setReservedCount(0);
                return result;
            }

            LinkedHashSet<Long> orderedPackageIds = authoritativePlan.getPackageIds().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            result.setRequestedCount(orderedPackageIds.size());

            for (Long packageId : orderedPackageIds) {
                try {
                    PackageReservation reservation = reservePackageInternal(packageId, deliveryPersonID);
                    Package aPackage = reservation.getaPackage();
                    if (aPackage.getSender() != null && aPackage.getSender().getId() != null) {
                        notificationService.createAndDispatch(aPackage.getSender().getId(), NOTIFICATION_EVENT_TYPE.PACKAGE_RESERVED, "/package?id=" + aPackage.getReference(), "{\"packageReference\":\"" + aPackage.getReference() + "\"}");
                    }
                    primePackageForAsyncNotifications(aPackage);
                    runPackageMailTask(() -> {
                        sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_RESERVATION_SENDER.getType(),
                                messageSource.getMessage("email.subject.packageReserved", null, locale), EMAIL_TYPE.PACKAGE_RESERVATION_SENDER);
                        sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_RESERVATION_DELIVERY.getType(),
                                messageSource.getMessage("email.subject.packageReservationCofirm", null, locale), EMAIL_TYPE.PACKAGE_RESERVATION_DELIVERY);
                    });
                    result.getReservedPackageIds().add(packageId);
                } catch (ResponseStatusException ex) {
                    result.getSkippedPackages().put(packageId, ex.getReason() == null ? ex.getStatusCode().toString() : ex.getReason());
                } catch (Exception ex) {
                    logger.error("Unable to reserve package {} in planned batch for delivery person {}: {}",
                            packageId, deliveryPersonID, ex.getMessage(), ex);
                    result.getSkippedPackages().put(packageId,
                            ex.getMessage() == null || ex.getMessage().isBlank()
                                    ? ex.getClass().getSimpleName()
                                    : ex.getClass().getSimpleName() + ": " + ex.getMessage());
                }
            }

            result.setReservedCount(result.getReservedPackageIds().size());
            RoutePlanDTO reservedRoutePlan = filterRoutePlanToReserved(authoritativePlan, result.getReservedPackageIds());
            if (reservedRoutePlan != null && !result.getReservedPackageIds().isEmpty()) {
                DeliveryRoute deliveryRoute = createDeliveryRoute(deliveryPersonID, reservedRoutePlan);
                reservedRoutePlan = toRoutePlanDTO(deliveryRoute);
            }
            result.setReservedRoutePlan(reservedRoutePlan);
            return result;
        });
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "packagesByStatus", allEntries = true),
            @CacheEvict(value = "packagesAroundGrouped", allEntries = true),
            @CacheEvict(value = "packagesAroundMe", allEntries = true),
            @CacheEvict(value = "packagesAroundDestination", allEntries = true),
            @CacheEvict(value = "packagesByDeliveryPerson", allEntries = true),
            @CacheEvict(value = "packagesBySender", allEntries = true),
            @CacheEvict(value = "userWithOngoingDelivery", allEntries = true),
            @CacheEvict(value = "packagesAdminDashboardSummary", allEntries = true),
            @CacheEvict(value = "packagesAdminFinancialDashboard", allEntries = true)
    })
    public void pickUpPackage(Long packageID, Long deliveryPersonID, String pickUpOTP, Locale locale, PositionDTO currentPosition) throws NoSuchAlgorithmException {
        recordPackageOperationChecked("pickup", () -> {
            Package aPackage = packages.findByIdForUpdate(packageID)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
            Optional<PackageReservation> packageReservation = aPackage.getPackageReservations().stream()
                    .filter(currentReservation -> currentReservation.getDeliveryPerson() != null
                            && currentReservation.getDeliveryPerson().getId().equals(deliveryPersonID))
                    .findFirst();
            if(packageReservation.isPresent() && Objects.equals(packageReservation.get().getPickUpOTP(), pickUpOTP)) {
                validateCourierPresenceAtStop(deliveryPersonID, aPackage, DELIVERY_ROUTE_STOP_KIND.PICKUP, currentPosition);
                aPackage.setStatus(PACKAGE_STATUS.PICKEDUP);
                packageReservation.get().setDeliveryOTP(OTPHelper.generateOTP(OTPSecret, System.currentTimeMillis()));
                packages.saveAndFlush(aPackage);
                markRouteStopCompleted(deliveryPersonID, packageID, DELIVERY_ROUTE_STOP_KIND.PICKUP);
                if (aPackage.getSender() != null && aPackage.getSender().getId() != null) {
                    notificationService.createAndDispatch(aPackage.getSender().getId(), NOTIFICATION_EVENT_TYPE.PACKAGE_PICKED_UP, "/package?id=" + aPackage.getReference(), "{\"packageReference\":\"" + aPackage.getReference() + "\"}");
                }
                if (packageReservation.get().getDeliveryPerson() != null && packageReservation.get().getDeliveryPerson().getId() != null) {
                    notificationService.createAndDispatch(packageReservation.get().getDeliveryPerson().getId(), NOTIFICATION_EVENT_TYPE.PACKAGE_PICKUP_SUCCESS, "/package?id=" + aPackage.getReference(), "{\"packageReference\":\"" + aPackage.getReference() + "\"}", aPackage.getReference());
                }
                Address receiverAddressPickup = getArrivalAddress(aPackage.getAddresses());
                if (receiverAddressPickup != null && receiverAddressPickup.getResidents() != null && receiverAddressPickup.getResidents().getId() != null) {
                    notificationService.createAndDispatch(receiverAddressPickup.getResidents().getId(), NOTIFICATION_EVENT_TYPE.PACKAGE_PICKUP_SUCCESS, "/package?id=" + aPackage.getReference(), "{\"packageReference\":\"" + aPackage.getReference() + "\",\"otp\":\"" + packageReservation.get().getDeliveryOTP() + "\"}", aPackage.getReference(), packageReservation.get().getDeliveryOTP());
                }
                primePackageForAsyncNotifications(aPackage);
                runPackageMailTask(() -> {
                    sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_PICKUP_SENDER.getType(),
                            messageSource.getMessage("email.subject.packagePickup", null, locale),EMAIL_TYPE.PACKAGE_PICKUP_SENDER);
                    sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_PICKUP_RECEIVER.getType(),
                            messageSource.getMessage("email.subject.packagePickupR", null, locale),EMAIL_TYPE.PACKAGE_PICKUP_RECEIVER);
                    sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_PICKUP_DELIVERY.getType(),
                            messageSource.getMessage("email.subject.packagePickupCofirm", null, locale),EMAIL_TYPE.PACKAGE_PICKUP_DELIVERY);
                });
                return null;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid pickup OTP");
        });
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "packagesByStatus", allEntries = true),
            @CacheEvict(value = "packagesAroundGrouped", allEntries = true),
            @CacheEvict(value = "packagesAroundMe", allEntries = true),
            @CacheEvict(value = "packagesAroundDestination", allEntries = true),
            @CacheEvict(value = "packagesByDeliveryPerson", allEntries = true),
            @CacheEvict(value = "packagesBySender", allEntries = true),
            @CacheEvict(value = "userWithOngoingDelivery", allEntries = true),
            @CacheEvict(value = "packagesAdminDashboardSummary", allEntries = true),
            @CacheEvict(value = "packagesAdminFinancialDashboard", allEntries = true)
    })
    public void deliverPackage(Long packageID, Long deliveryPersonID, String pickUpOTP, Locale locale, PositionDTO currentPosition) throws NoSuchAlgorithmException {
        recordPackageOperationChecked("deliver", () -> {
            Package aPackage = packages.findByIdForUpdate(packageID)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
            Optional<PackageReservation> packageReservation = aPackage.getPackageReservations().stream()
                    .filter(currentReservation -> currentReservation.getDeliveryPerson() != null
                            && currentReservation.getDeliveryPerson().getId().equals(deliveryPersonID))
                    .findFirst();
            if(packageReservation.isPresent() && Objects.equals(packageReservation.get().getDeliveryOTP(), pickUpOTP)) {
                validateCourierPresenceAtStop(deliveryPersonID, aPackage, DELIVERY_ROUTE_STOP_KIND.DROPOFF, currentPosition);
                aPackage.setStatus(PACKAGE_STATUS.DELIVERED);
                packageReservation.get().setStatus(PACKAGE_RESERVATION_STATUS.FINISHED);
                packages.saveAndFlush(aPackage);
                markRouteStopCompleted(deliveryPersonID, packageID, DELIVERY_ROUTE_STOP_KIND.DROPOFF);
                createCourierPayoutIfNeeded(aPackage, packageReservation.get().getDeliveryPerson());
                if (aPackage.getSender() != null && aPackage.getSender().getId() != null) {
                    notificationService.createAndDispatch(aPackage.getSender().getId(), NOTIFICATION_EVENT_TYPE.PACKAGE_DELIVERED, "/package?id=" + aPackage.getReference(), "{\"packageReference\":\"" + aPackage.getReference() + "\"}");
                }
                if (packageReservation.get().getDeliveryPerson() != null && packageReservation.get().getDeliveryPerson().getId() != null) {
                    notificationService.createAndDispatch(packageReservation.get().getDeliveryPerson().getId(), NOTIFICATION_EVENT_TYPE.PACKAGE_DELIVERY_SUCCESS, "/package?id=" + aPackage.getReference(), "{\"packageReference\":\"" + aPackage.getReference() + "\"}", aPackage.getReference());
                }
                Address receiverAddressDelivery = getArrivalAddress(aPackage.getAddresses());
                if (receiverAddressDelivery != null && receiverAddressDelivery.getResidents() != null && receiverAddressDelivery.getResidents().getId() != null) {
                    notificationService.createAndDispatch(receiverAddressDelivery.getResidents().getId(), NOTIFICATION_EVENT_TYPE.PACKAGE_DELIVERED, "/package?id=" + aPackage.getReference(), "{\"packageReference\":\"" + aPackage.getReference() + "\"}");
                }
                primePackageForAsyncNotifications(aPackage);
                runPackageMailTask(() -> {
                    sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_DELIVERY_RECEIVER.getType(),
                            messageSource.getMessage("email.subject.packageDelivered", null, locale),EMAIL_TYPE.PACKAGE_DELIVERY_RECEIVER);
                    sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_DELIVERY_SENDER.getType(),
                            messageSource.getMessage("email.subject.packageDelivered", null, locale),EMAIL_TYPE.PACKAGE_DELIVERY_SENDER);
                });
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
        List<PackageReservation> packageReservation_ = aPackage.getPackageReservations().stream()
                .filter(packageReservation -> Objects.equals(packageReservation.getDeliveryOTP(), deliveryOTP))
                .filter(packageReservation -> packageReservation.getDeliveryPerson() != null
                        && Objects.equals(packageReservation.getDeliveryPerson().getId(), deliveryPersonID))
                .collect(Collectors.toList());
        if(Boolean.TRUE.equals(aPackage.getGuestMode())){
            return CHECK_STATUS.KO;
        }
        if(packageReservation_.size()>0) {
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
    @Caching(evict = {
            @CacheEvict(value = "packagesByStatus", allEntries = true),
            @CacheEvict(value = "packagesAroundGrouped", allEntries = true),
            @CacheEvict(value = "packagesAroundMe", allEntries = true),
            @CacheEvict(value = "packagesAroundDestination", allEntries = true),
            @CacheEvict(value = "packagesByDeliveryPerson", allEntries = true),
            @CacheEvict(value = "packagesBySender", allEntries = true),
            @CacheEvict(value = "userWithOngoingDelivery", allEntries = true),
            @CacheEvict(value = "packagesAdminFinancialDashboard", allEntries = true)
    })
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
    @Cacheable(value = "packagesByStatus", key = "#status.name()", sync = true)
    public List<PackageDTO> findPackagesByStatus(PACKAGE_STATUS status) {
        long startedAt = System.currentTimeMillis();
        int effectiveLimit = Math.max(1, maxPackagesByStatusResults);
        List<Long> packageIds = this.packages.findPackageIdsByStatus(status, PageRequest.of(0, effectiveLimit)).getContent();
        logger.info("Loaded {} package ids for status {} with limit {}", packageIds.size(), status, effectiveLimit);
        List<PackageDTO> packageDTOS = loadRecentPackagesForDashboard(packageIds);
        logger.info("Returning {} packages for status {} in {} ms", packageDTOS.size(), status, System.currentTimeMillis() - startedAt);
        return packageDTOS;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "packagesByStatus", allEntries = true),
            @CacheEvict(value = "packagesAroundGrouped", allEntries = true),
            @CacheEvict(value = "packagesAroundMe", allEntries = true),
            @CacheEvict(value = "packagesAroundDestination", allEntries = true),
            @CacheEvict(value = "packagesByDeliveryPerson", allEntries = true),
            @CacheEvict(value = "packagesBySender", allEntries = true),
            @CacheEvict(value = "userWithOngoingDelivery", allEntries = true),
            @CacheEvict(value = "packagesAdminDashboardSummary", allEntries = true),
            @CacheEvict(value = "packagesAdminFinancialDashboard", allEntries = true)
    })
    public void updatePackageStatus(PACKAGE_STATUS status, Long id) {
        packages.updatePackagesStatus(status,id);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "packagesByDeliveryPerson", key = "#deliveryPersonID", sync = true)
    public Map<PACKAGE_STATUS, List<PackageDTO>> getPackagesByDeliveryPerson(Long deliveryPersonID) {
        List<Package> packageList = packages.findPackagesByDeliveryPerson(deliveryPersonID);
        List<PackageDTO> packageDTOS = packageList.stream()
                .map(this::toSummaryPackageDTO)
                .collect(Collectors.toList());
        Map<PACKAGE_STATUS, List<PackageDTO>> groupedPackages = packageDTOS.parallelStream()
                .collect(Collectors.groupingByConcurrent(packaged -> {
                    return packaged.getStatus();
                }));
        return groupedPackages;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "packagesBySender", key = "#senderID", sync = true)
    public Map<PACKAGE_STATUS, List<PackageDTO>> getPackagesBySender(Long senderID) {
        List<Package> packageList = packages.findPackagesBySender(senderID);
        List<PackageDTO> packageDTOS = packageList.stream()
                .map(this::toSummaryPackageDTO)
                .collect(Collectors.toList());
        return packageDTOS.parallelStream()
                .collect(Collectors.groupingByConcurrent(PackageDTO::getStatus));
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryReservationContextDTO getDeliveryReservationContext(Long packageId, Long deliveryPersonId) {
        PackageReservation reservation = packageReservations.findReservationContext(
                        packageId,
                        deliveryPersonId,
                        PACKAGE_RESERVATION_STATUS.ONGOING
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active reservation not found"));
        DeliveryReservationContextDTO contextDTO = new DeliveryReservationContextDTO();
        contextDTO.setPackageId(reservation.getaPackage().getId());
        contextDTO.setPackageReference(reservation.getaPackage().getReference());
        contextDTO.setPackageStatus(reservation.getaPackage().getStatus());
        contextDTO.setReservationStatus(reservation.getStatus());
        contextDTO.setPickUpOTP(reservation.getPickUpOTP());
        contextDTO.setDeliveryOTP(reservation.getDeliveryOTP());
        return contextDTO;
    }

    private Package preparPackage(PackageDTO packageDTO, Locale locale, PackagePricingBreakdown pricingBreakdown) throws MalformedURLException, FileNotFoundException {
        applyPricing(packageDTO, pricingBreakdown);
        Package aPackage = modelMapper.map(packageDTO, Package.class);
        aPackage.setPackageSizeCategory(packageDTO.getPackageSizeCategory());
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
            if (hasUsableCoordinates(addressDTO)) {
                logger.debug("Skipping geocoding for {} address because coordinates are already present: lat={}, lng={}",
                        addressDTO.getType(), addressDTO.getLatitude(), addressDTO.getLongitude());
                return;
            }
            try {
                long startedAt = System.currentTimeMillis();
                GeoHelper.AddressGeoCoding(geoApiContext, addressDTO);
                logger.info("Geocoded {} address '{}' to lat={}, lng={} in {} ms",
                        addressDTO.getType(),
                        summarizeAddress(addressDTO),
                        addressDTO.getLatitude(),
                        addressDTO.getLongitude(),
                        System.currentTimeMillis() - startedAt);
            } catch (IOException e) {
                logger.error("I/O failure while geocoding {} address '{}': {}",
                        addressDTO.getType(), summarizeAddress(addressDTO), e.getMessage(), e);
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Geocoding interrupted for {} address '{}': {}",
                        addressDTO.getType(), summarizeAddress(addressDTO), e.getMessage(), e);
                throw new RuntimeException(e);
            } catch (ApiException e) {
                logger.error("Geo API failure while geocoding {} address '{}': {}",
                        addressDTO.getType(), summarizeAddress(addressDTO), e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    private boolean hasUsableCoordinates(AddressDTO addressDTO) {
        if (addressDTO == null || addressDTO.getLatitude() == null || addressDTO.getLongitude() == null) {
            return false;
        }

        double latitude = addressDTO.getLatitude().doubleValue();
        double longitude = addressDTO.getLongitude().doubleValue();
        return !(Math.abs(latitude) < 1e-9 && Math.abs(longitude) < 1e-9);
    }

    private double resolveDistanceInKilometers(PackageDTO aPackage) {
        AddressDTO departureAddress = getDepartureAddress(aPackage.getAddresses());
        AddressDTO arrivalAddress = getArrivalAddress(aPackage.getAddresses());
        String packageReference = aPackage.getReference() == null ? "draft-package" : aPackage.getReference();
        long startedAt = System.currentTimeMillis();
        String routeCacheKey = buildRouteCacheKey(departureAddress, arrivalAddress);

        CachedRouteDistance cachedRouteDistance = getCachedRouteDistance(routeCacheKey);
        if (cachedRouteDistance != null) {
            aPackage.setDistanceToDestination(cachedRouteDistance.formattedDistance());
            logger.info("Resolved route distance from cache for package {}: {} km in {} ms",
                    packageReference,
                    String.format(Locale.ROOT, "%.2f", cachedRouteDistance.distanceInKilometers()),
                    System.currentTimeMillis() - startedAt);
            return cachedRouteDistance.distanceInKilometers();
        }

        DistanceMatrix distanceMatrix = tryResolveDistanceMatrix(departureAddress, arrivalAddress);
        if (distanceMatrix != null) {
            aPackage.setDistanceToDestination(formatDistanceMatrixValue(distanceMatrix));
            double distanceInKilometers = extractDistanceInKilometers(distanceMatrix);
            cacheRouteDistance(routeCacheKey, distanceInKilometers, aPackage.getDistanceToDestination());
            logger.info("Resolved route distance through provider for package {}: {} km in {} ms",
                    packageReference,
                    String.format(Locale.ROOT, "%.2f", distanceInKilometers),
                    System.currentTimeMillis() - startedAt);
            return distanceInKilometers;
        }

        if (hasUsableCoordinates(departureAddress) && hasUsableCoordinates(arrivalAddress)) {
            double straightLineMeters = haversineMeters(
                    departureAddress.getLatitude().doubleValue(),
                    departureAddress.getLongitude().doubleValue(),
                    arrivalAddress.getLatitude().doubleValue(),
                    arrivalAddress.getLongitude().doubleValue()
            );
            double straightLineKilometers = straightLineMeters / 1000.0;
            aPackage.setDistanceToDestination("approx/" + formatApproximateDistance(straightLineMeters));
            cacheRouteDistance(routeCacheKey, straightLineKilometers, aPackage.getDistanceToDestination());
            logger.warn(
                    "Falling back to straight-line distance for package {} between [{}] and [{}]: {} km after {} ms",
                    packageReference,
                    departureAddress,
                    arrivalAddress,
                    String.format(Locale.ROOT, "%.2f", straightLineKilometers),
                    System.currentTimeMillis() - startedAt
            );
            return straightLineKilometers;
        }

        logger.error("Unable to resolve route distance for package {} from '{}' to '{}'",
                packageReference, summarizeAddress(departureAddress), summarizeAddress(arrivalAddress));
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to resolve route distance");
    }

    private DistanceMatrix tryResolveDistanceMatrix(AddressDTO departureAddress, AddressDTO arrivalAddress) {
        if (!isGoogleMapsDistanceProviderEnabled()) {
            logger.debug("Skipping Google Maps distance provider because it is temporarily disabled");
            return null;
        }

        if (hasUsableCoordinates(departureAddress) && hasUsableCoordinates(arrivalAddress)) {
            try {
                long startedAt = System.currentTimeMillis();
                DistanceMatrix distanceMatrix = GeoHelper.getDistanceByCoordinates(
                        geoApiContext,
                        departureAddress.getLatitude().doubleValue(),
                        departureAddress.getLongitude().doubleValue(),
                        arrivalAddress.getLatitude().doubleValue(),
                        arrivalAddress.getLongitude().doubleValue()
                );
                logger.info("Distance provider succeeded from coordinates between '{}' and '{}' in {} ms",
                        summarizeAddress(departureAddress),
                        summarizeAddress(arrivalAddress),
                        System.currentTimeMillis() - startedAt);
                return distanceMatrix;
            } catch (IOException | InterruptedException | ApiException exception) {
                logger.warn(
                        "Unable to resolve route distance from coordinates between '{}' and '{}' . Falling back to address mode: {}",
                        summarizeAddress(departureAddress),
                        summarizeAddress(arrivalAddress),
                        exception.getMessage(),
                        exception
                );
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                if (shouldDisableGoogleMapsDistanceProvider(exception)) {
                    temporarilyDisableGoogleMapsDistanceProvider(exception);
                    return null;
                }
            }
        }

        if (!isGoogleMapsDistanceProviderEnabled()) {
            return null;
        }

        try {
            long startedAt = System.currentTimeMillis();
            DistanceMatrix distanceMatrix = GeoHelper.getDistanceByAddress(geoApiContext, departureAddress.toString(), arrivalAddress.toString());
            logger.info("Distance provider succeeded from addresses between '{}' and '{}' in {} ms",
                    summarizeAddress(departureAddress),
                    summarizeAddress(arrivalAddress),
                    System.currentTimeMillis() - startedAt);
            return distanceMatrix;
        } catch (IOException | InterruptedException | ApiException exception) {
            logger.warn(
                    "Unable to resolve route distance from addresses between '{}' and '{}': {}",
                    summarizeAddress(departureAddress),
                    summarizeAddress(arrivalAddress),
                    exception.getMessage(),
                    exception
            );
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (shouldDisableGoogleMapsDistanceProvider(exception)) {
                temporarilyDisableGoogleMapsDistanceProvider(exception);
            }
            return null;
        }
    }

    private boolean isGoogleMapsDistanceProviderEnabled() {
        return System.currentTimeMillis() >= googleMapsDistanceProviderDisabledUntilEpochMs.get();
    }

    private boolean shouldDisableGoogleMapsDistanceProvider(Exception exception) {
        if (exception instanceof RequestDeniedException) {
            return true;
        }
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        return normalizedMessage.contains("billing")
                || normalizedMessage.contains("request_denied")
                || normalizedMessage.contains("api key")
                || normalizedMessage.contains("forbidden");
    }

    private void temporarilyDisableGoogleMapsDistanceProvider(Exception exception) {
        long disabledUntil = System.currentTimeMillis() + (googleMapsDistanceProviderCooldownSeconds * 1000L);
        long previousValue = googleMapsDistanceProviderDisabledUntilEpochMs.getAndUpdate(current -> Math.max(current, disabledUntil));
        if (disabledUntil > previousValue) {
            logger.warn("Disabling Google Maps distance provider until {} after non-retryable failure: {}",
                    Instant.ofEpochMilli(disabledUntil),
                    exception.getMessage());
        }
    }

    private String summarizeAddress(AddressDTO addressDTO) {
        if (addressDTO == null) {
            return "n/a";
        }
        return String.join(", ",
                Stream.of(addressDTO.getLine1(), addressDTO.getZipCode(), addressDTO.getTown(), addressDTO.getCountry())
                        .filter(Objects::nonNull)
                        .filter(value -> !value.isBlank())
                        .toList());
    }

    private CachedRouteDistance getCachedRouteDistance(String routeCacheKey) {
        if (routeCacheKey == null || routeCacheKey.isBlank()) {
            return null;
        }
        CachedRouteDistance cachedRouteDistance = distanceCache.get(routeCacheKey);
        if (cachedRouteDistance == null) {
            return null;
        }
        if (cachedRouteDistance.expiresAtEpochMs() < System.currentTimeMillis()) {
            distanceCache.remove(routeCacheKey, cachedRouteDistance);
            return null;
        }
        return cachedRouteDistance;
    }

    private void cacheRouteDistance(String routeCacheKey, double distanceInKilometers, String formattedDistance) {
        if (routeCacheKey == null || routeCacheKey.isBlank() || distanceCacheTtlSeconds <= 0) {
            return;
        }
        evictExpiredDistanceCacheEntries();
        if (distanceCache.size() >= maxDistanceCacheEntries && !distanceCache.containsKey(routeCacheKey)) {
            evictOneDistanceCacheEntry();
        }
        long expiresAtEpochMs = System.currentTimeMillis() + (distanceCacheTtlSeconds * 1000L);
        distanceCache.put(routeCacheKey, new CachedRouteDistance(distanceInKilometers, formattedDistance, expiresAtEpochMs));
    }

    private void evictExpiredDistanceCacheEntries() {
        long now = System.currentTimeMillis();
        distanceCache.entrySet().removeIf(entry -> entry.getValue().expiresAtEpochMs() < now);
    }

    private void evictOneDistanceCacheEntry() {
        Iterator<Map.Entry<String, CachedRouteDistance>> iterator = distanceCache.entrySet().iterator();
        if (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    private String buildRouteCacheKey(AddressDTO departureAddress, AddressDTO arrivalAddress) {
        return normalizedRouteEndpoint(departureAddress) + "->" + normalizedRouteEndpoint(arrivalAddress);
    }

    private String normalizedRouteEndpoint(AddressDTO addressDTO) {
        if (addressDTO == null) {
            return "n/a";
        }
        if (hasUsableCoordinates(addressDTO)) {
            return "coord:" + normalizeRouteCoordinate(addressDTO.getLatitude().doubleValue()) + "," + normalizeRouteCoordinate(addressDTO.getLongitude().doubleValue());
        }
        return "addr:" + Stream.of(addressDTO.getLine1(), addressDTO.getZipCode(), addressDTO.getTown(), addressDTO.getCountry())
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining("|"));
    }

    private String normalizeRouteCoordinate(double value) {
        return BigDecimal.valueOf(value)
                .setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private PackagePricingBreakdown calculatePricingBreakdown(PackageDTO packageDTO, double distanceInKilometers) {
        return PackageDeliveryPriceCalculator.calculatePricingBreakdown(distanceInKilometers, packageDTO);
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
        packageDTO.setPackageSizeCategory(normalizePackageSizeCategory(packageDTO.getPackageSizeCategory()));
        if (packageDTO.getPackageSizeCategory() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Package size category is required");
        }
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

    private String formatApproximateDistance(double distanceMeters) {
        if (distanceMeters <= 0d || Double.isNaN(distanceMeters) || Double.isInfinite(distanceMeters)) {
            return "-/-";
        }
        double distanceKm = distanceMeters / 1000d;
        long approxMinutes = Math.max(1L, Math.round(distanceKm / 35d * 60d));
        if (distanceKm < 1d) {
            return approxMinutes + " min/" + Math.round(distanceMeters) + " m";
        }
        return approxMinutes + " min/" + String.format(Locale.US, "%.1f km", distanceKm);
    }

    private void sendPackageCreationEMail(Package aPackage, Locale locale, String template, String subject, EMAIL_TYPE type){
        if (shouldSkipPackageMail()) {
            return;
        }
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
            if (!packageDocumentStorageService.exists(attachement)) {
                logger.warn("Skipping missing label attachment for package {}: {}", aPackage.getReference(), attachement);
                attachement = null;
            } else {
                try {
                    Path materializedAttachment = packageDocumentStorageService.materializeToTempFile(attachement);
                    attachement = materializedAttachment.toString();
                } catch (IOException exception) {
                    logger.warn("Unable to materialize label attachment for package {}: {}", aPackage.getReference(), exception.getMessage());
                    attachement = null;
                }
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
            handlePackageMailFailure(e, aPackage.getReference());
            logger.warn("Unable to send package email {} for package {}: {}", type, aPackage.getReference(), e.getMessage(), e);
        }
    }

    private void runPackageMailTask(Runnable task) {
        if (!emailNotificationsEnabled || shouldSkipPackageMail()) {
            return;
        }
        submitPackageAsyncTask(() -> runPackageMailInline(task), packageMailTaskExecutor, "package-mail");
    }

    private void runPackageMailInline(Runnable task) {
        if (shouldSkipPackageMail()) {
            return;
        }
        task.run();
    }

    private boolean shouldSkipPackageMail() {
        return !emailNotificationsEnabled || System.currentTimeMillis() < emailNotificationsDisabledUntilEpochMs.get();
    }

    private void handlePackageMailFailure(Exception exception, String packageReference) {
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase(Locale.ROOT);
        if (message.contains("too many login attempts")
                || message.contains("authenticationfailed")
                || message.contains("authentication failed")) {
            long disabledUntil = System.currentTimeMillis() + (emailNotificationCooldownSeconds * 1000L);
            emailNotificationsDisabledUntilEpochMs.set(disabledUntil);
            logger.warn("Temporarily disabling package email notifications until {} after failure on package {}",
                    new Date(disabledUntil), packageReference);
        }
    }

    private void submitPackageAsyncTask(Runnable task, Executor executor, String taskName) {
        try {
            CompletableFuture.runAsync(task, executor);
        } catch (RejectedExecutionException exception) {
            logger.warn("Dropping async task {} because the executor is saturated", taskName);
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
                // Using Locale.FRANCE as default for error messages if not specified
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, messageSource.getMessage("package.error.notFound", new Object[]{reference}, Locale.FRANCE));
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
    public PackageDTO findTrackingSubscriptionPackage(String reference, String guestAccessToken) {
        return recordPackageOperation("trackingSubscriptionLookup", () -> {
            if (reference == null || reference.isBlank() || "undefined".equalsIgnoreCase(reference)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Package reference is required");
            }
            Package aPackage = packages.findPackageByReference(reference);
            if (aPackage == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found for reference: " + reference);
            }
            if (guestAccessToken != null && !guestAccessToken.isBlank()
                    && !Objects.equals(aPackage.getGuestAccessToken(), guestAccessToken)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid guest access token");
            }

            PackageDTO packageDTO = new PackageDTO();
            packageDTO.setId(aPackage.getId());
            packageDTO.setReference(aPackage.getReference());
            packageDTO.setGuestMode(aPackage.getGuestMode());
            packageDTO.setGuestAccessToken(aPackage.getGuestAccessToken());
            packageDTO.setLastPositionLatitude(aPackage.getLastPositionLatitude());
            packageDTO.setLastPositionLongitude(aPackage.getLastPositionLongitude());
            return packageDTO;
        });
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
                byte[] data = packageDocumentStorageService.readBytes(document.getDocURL());
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
    @Cacheable(value = "userWithOngoingDelivery", key = "#userId", sync = true)
    public boolean isUserWithOngoingDelivery(Long userId) {
        return deliveryRoutes.existsByDeliveryPersonAndStatuses(userId, ACTIVE_DELIVERY_ROUTE_STATUSES);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationAvailabilityDTO getReservationAvailability(Long deliveryPersonId) {
        ReservationAvailabilityDTO dto = new ReservationAvailabilityDTO();
        if (deliveryPersonId == null) {
            dto.setCanReserve(false);
            dto.setReason("missingDeliveryPersonId");
            return dto;
        }
        User user = users.findById(deliveryPersonId).orElse(null);
        if (user == null) {
            dto.setCanReserve(false);
            dto.setReason("deliveryPersonNotFound");
            return dto;
        }

        int maxReservations = resolveUserCapacity(user);
        int activeReservations = Math.toIntExact(packageReservations.countActiveReservationsByDeliveryPerson(
                deliveryPersonId,
                PACKAGE_RESERVATION_STATUS.ONGOING,
                EnumSet.of(PACKAGE_STATUS.RESERVED, PACKAGE_STATUS.PICKEDUP, PACKAGE_STATUS.INDELIVERY)
        ));
        boolean activeRouteBlocking = deliveryRoutes.existsByDeliveryPersonAndStatuses(deliveryPersonId, ACTIVE_DELIVERY_ROUTE_STATUSES);
        boolean capacityReached = activeReservations >= maxReservations;
        Optional<CourierPenalty> activeSuspension = findActiveSuspension(deliveryPersonId);

        dto.setActiveReservations(activeReservations);
        dto.setMaxReservations(maxReservations);
        dto.setActiveRouteBlocking(activeRouteBlocking);
        dto.setCapacityReached(capacityReached);
        activeSuspension.ifPresent(penalty -> {
            dto.setSuspensionUntil(penalty.getSuspensionUntil() == null ? null : penalty.getSuspensionUntil().toInstant().toString());
            dto.setFinancialPenaltyAmount(penalty.getFinancialPenaltyAmount());
            dto.setFinancialPenaltyCurrency(penalty.getCurrency());
        });
        dto.setCanReserve(activeSuspension.isEmpty() && !activeRouteBlocking && !capacityReached);
        if (activeSuspension.isPresent()) {
            dto.setReason("courierSuspended");
        } else if (activeRouteBlocking) {
            dto.setReason("activeRoute");
        } else if (capacityReached) {
            dto.setReason("capacityReached");
        } else {
            dto.setReason("available");
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public RoutePlanDTO getActiveDeliveryRoute(Long deliveryPersonId) {
        return deliveryRoutes.findFirstByDeliveryPersonAndStatuses(deliveryPersonId, ACTIVE_DELIVERY_ROUTE_STATUSES)
                .map(this::toRoutePlanDTO)
                .orElse(null);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userWithOngoingDelivery", allEntries = true)
    public RoutePlanDTO startActiveDeliveryRoute(Long deliveryPersonId) {
        DeliveryRoute deliveryRoute = deliveryRoutes.findFirstByDeliveryPersonAndStatuses(deliveryPersonId, ACTIVE_DELIVERY_ROUTE_STATUSES)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active delivery route not found"));
        if (DELIVERY_ROUTE_STATUS.ACTIVE.equals(deliveryRoute.getStatus())) {
            return toRoutePlanDTO(deliveryRoute);
        }
        if (!DELIVERY_ROUTE_STATUS.PLANNED.equals(deliveryRoute.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "errorActiveRouteStartNotAllowed");
        }
        if (deliveryRouteStops.countByRouteAndStatus(deliveryRoute.getId(), DELIVERY_ROUTE_STOP_STATUS.PENDING) == 0) {
            persistCourierPenalty(
                    deliveryRoute.getDeliveryPerson(),
                    deliveryRoute,
                    null,
                    COURIER_PENALTY_TYPE.ROUTE_STARTED_WITHOUT_PENDING_STOP,
                    2,
                    "routeStartWithoutPendingStop",
                    "{\"deliveryRouteId\":" + deliveryRoute.getId() + "}",
                    false
            );
            throw new ResponseStatusException(HttpStatus.CONFLICT, "errorActiveRouteStartNotAllowed");
        }
        deliveryRoute.setStatus(DELIVERY_ROUTE_STATUS.ACTIVE);
        deliveryRoute.setStartedAt(Timestamp.valueOf(LocalDateTime.now()));
        return toRoutePlanDTO(deliveryRoutes.saveAndFlush(deliveryRoute));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "packagesByStatus", allEntries = true),
            @CacheEvict(value = "packagesAroundGrouped", allEntries = true),
            @CacheEvict(value = "packagesAroundMe", allEntries = true),
            @CacheEvict(value = "packagesAroundDestination", allEntries = true),
            @CacheEvict(value = "packagesByDeliveryPerson", allEntries = true),
            @CacheEvict(value = "packagesBySender", allEntries = true),
            @CacheEvict(value = "userWithOngoingDelivery", allEntries = true)
    })
    public PackageDTO cancelReservation(Long packageID, Long deliveryPersonID) {
        PackageReservation reservation = packageReservations.findByPackageAndDeliveryPersonAndStatus(
                        packageID,
                        deliveryPersonID,
                        PACKAGE_RESERVATION_STATUS.ONGOING
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
        Package aPackage = reservation.getaPackage();
        if (aPackage == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found");
        }
        if (!PACKAGE_STATUS.RESERVED.equals(aPackage.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "errorReservationCancellationNotAllowed");
        }
        Optional<DeliveryRoute> activeRoute = deliveryRoutes.findFirstByDeliveryPersonAndStatuses(deliveryPersonID, ACTIVE_DELIVERY_ROUTE_STATUSES);
        if (activeRoute.isPresent()) {
            cancelReservedPackageFromRoute(activeRoute.get(), aPackage, reservation);
            return toPackageDTO(aPackage);
        }
        reservation.setStatus(PACKAGE_RESERVATION_STATUS.CANCELED);
        aPackage.setStatus(PACKAGE_STATUS.NEW);
        aPackage.setReservationDate(null);
        aPackage.setIsSoftLockedBy(null);
        aPackage.setSoftLockExpiresAt(null);
        packageReservations.save(reservation);
        packages.saveAndFlush(aPackage);
        return toPackageDTO(aPackage);
    }

    private void cancelReservedPackageFromRoute(DeliveryRoute deliveryRoute, Package aPackage, PackageReservation reservation) {
        List<DeliveryRouteStop> allStops = deliveryRouteStops.findByRouteIdOrderByStopOrder(deliveryRoute.getId());
        List<DeliveryRouteStop> stopsToCancel = allStops.stream()
                .filter(stop -> stop.getaPackage() != null && Objects.equals(stop.getaPackage().getId(), aPackage.getId()))
                .collect(Collectors.toList());
        if (stopsToCancel.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "errorReservationCancellationNotAllowed");
        }
        boolean hasProcessedStop = stopsToCancel.stream()
                .anyMatch(stop -> !DELIVERY_ROUTE_STOP_STATUS.PENDING.equals(stop.getStatus()));
        if (hasProcessedStop) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "errorReservationCancellationNotAllowed");
        }
        persistCourierPenalty(
                deliveryRoute.getDeliveryPerson(),
                deliveryRoute,
                aPackage,
                COURIER_PENALTY_TYPE.ROUTE_PACKAGE_CANCELLED,
                1,
                "routePackageCancelled",
                "{\"deliveryRouteId\":" + deliveryRoute.getId() + ",\"packageId\":" + aPackage.getId() + "}",
                true
        );

        reservation.setStatus(PACKAGE_RESERVATION_STATUS.CANCELED);
        aPackage.setStatus(PACKAGE_STATUS.NEW);
        aPackage.setReservationDate(null);
        aPackage.setIsSoftLockedBy(null);
        aPackage.setSoftLockExpiresAt(null);
        packageReservations.save(reservation);
        packages.save(aPackage);

        deliveryRouteStops.deleteAll(stopsToCancel);
        List<DeliveryRouteStop> remainingStops = allStops.stream()
                .filter(stop -> stopsToCancel.stream().noneMatch(cancelled -> Objects.equals(cancelled.getId(), stop.getId())))
                .collect(Collectors.toList());
        if (remainingStops.isEmpty()) {
            deliveryRoute.setStatus(DELIVERY_ROUTE_STATUS.CANCELLED);
            deliveryRoute.setCompletedAt(Timestamp.valueOf(LocalDateTime.now()));
            deliveryRoute.setTotalDistanceMeters(0d);
            deliveryRoute.setEstimatedDurationMinutes(0d);
            deliveryRoute.setTotalCourierPayout(0d);
            deliveryRoute.setTotalWeightKg(0d);
            deliveryRoute.setTotalVolumeCm3(0d);
            deliveryRoutes.saveAndFlush(deliveryRoute);
            return;
        }

        for (int index = 0; index < remainingStops.size(); index++) {
            remainingStops.get(index).setStopOrder(index + 1);
        }
        refreshDeliveryRouteMetrics(deliveryRoute, remainingStops);
        deliveryRouteStops.saveAll(remainingStops);
        deliveryRoutes.saveAndFlush(deliveryRoute);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "packagesByStatus", allEntries = true),
            @CacheEvict(value = "packagesAroundGrouped", allEntries = true),
            @CacheEvict(value = "packagesAroundMe", allEntries = true),
            @CacheEvict(value = "packagesAroundDestination", allEntries = true),
            @CacheEvict(value = "packagesByDeliveryPerson", allEntries = true),
            @CacheEvict(value = "packagesBySender", allEntries = true),
            @CacheEvict(value = "userWithOngoingDelivery", allEntries = true)
    })
    public RoutePlanDTO cancelActiveDeliveryRoute(Long deliveryPersonId) {
        DeliveryRoute deliveryRoute = deliveryRoutes.findFirstByDeliveryPersonAndStatuses(deliveryPersonId, ACTIVE_DELIVERY_ROUTE_STATUSES)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active delivery route not found"));
        if (!DELIVERY_ROUTE_STATUS.PLANNED.equals(deliveryRoute.getStatus())) {
            persistCourierPenalty(
                    deliveryRoute.getDeliveryPerson(),
                    deliveryRoute,
                    null,
                    COURIER_PENALTY_TYPE.ROUTE_CANCELLED_AFTER_START,
                    4,
                    "activeRouteCancellationAttemptAfterStart",
                    "{\"deliveryRouteId\":" + deliveryRoute.getId() + ",\"status\":\"" + deliveryRoute.getStatus() + "\"}",
                    true
            );
            throw new ResponseStatusException(HttpStatus.CONFLICT, "errorActiveRouteCancellationNotAllowed");
        }
        List<DeliveryRouteStop> stops = deliveryRouteStops.findByRouteIdOrderByStopOrder(deliveryRoute.getId());
        LinkedHashSet<Long> packageIds = stops.stream()
                .map(stop -> stop.getaPackage() == null ? null : stop.getaPackage().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (Long packageId : packageIds) {
            packageReservations.findByPackageAndDeliveryPersonAndStatus(packageId, deliveryPersonId, PACKAGE_RESERVATION_STATUS.ONGOING)
                    .ifPresent(reservation -> {
                        reservation.setStatus(PACKAGE_RESERVATION_STATUS.CANCELED);
                        packageReservations.save(reservation);
                    });
            packages.findByIdForUpdate(packageId).ifPresent(aPackage -> {
                if (PACKAGE_STATUS.RESERVED.equals(aPackage.getStatus())) {
                    aPackage.setStatus(PACKAGE_STATUS.NEW);
                    aPackage.setReservationDate(null);
                    aPackage.setIsSoftLockedBy(null);
                    aPackage.setSoftLockExpiresAt(null);
                    packages.save(aPackage);
                }
            });
        }
        stops.forEach(stop -> {
            if (DELIVERY_ROUTE_STOP_STATUS.PENDING.equals(stop.getStatus())) {
                stop.setStatus(DELIVERY_ROUTE_STOP_STATUS.SKIPPED);
            }
        });
        persistCourierPenalty(
                deliveryRoute.getDeliveryPerson(),
                deliveryRoute,
                null,
                COURIER_PENALTY_TYPE.ROUTE_CANCELLED_BEFORE_START,
                2,
                "plannedRouteCancelledBeforeStart",
                "{\"deliveryRouteId\":" + deliveryRoute.getId() + ",\"packageCount\":" + packageIds.size() + "}",
                true
        );
        deliveryRouteStops.saveAll(stops);
        deliveryRoute.setStatus(DELIVERY_ROUTE_STATUS.CANCELLED);
        deliveryRoute.setCompletedAt(Timestamp.valueOf(LocalDateTime.now()));
        deliveryRoutes.saveAndFlush(deliveryRoute);
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "packagesAdminMetrics", key = "'singleton'", sync = true)
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
    @Cacheable(value = "packagesAdminTrackingMetrics", key = "'singleton'", sync = true)
    public ServiceMetricsDTO loadAdminTrackingMetrics() {
        ServiceMetricsDTO metrics = new ServiceMetricsDTO();
        metrics.setServiceName("tracking-service");
        metrics.setUptimeSeconds(readGauge("process.uptime"));
        metrics.setHeapUsedMb(toMegabytes(readGauge("jvm.memory.used", "area", "heap")));
        metrics.setHeapMaxMb(toMegabytes(readGauge("jvm.memory.max", "area", "heap")));
        metrics.setCpuUsagePercent(toPercent(readGauge("system.cpu.usage")));
        metrics.setHttpRequestCount(sumTrackingHttpRequests());
        metrics.setOperationCallCount(sumTrackingOperations());
        metrics.setAsyncQueueSize(readGauge("quickdelivery.async.queue.size", "executor", "trackingAsyncTaskExecutor"));
        metrics.setAsyncActiveCount(readGauge("quickdelivery.async.active.count", "executor", "trackingAsyncTaskExecutor"));
        metrics.setOcrProcessedCount(null);
        return metrics;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "packagesAdminHttpBreakdown", key = "'singleton'", sync = true)
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
    @Cacheable(value = "packagesAdminDashboardSummary", key = "#year", sync = true)
    public AdminPackageDashboardSummaryDTO loadAdminDashboardSummary(int year) {
        AdminPackageDashboardSummaryDTO summary = new AdminPackageDashboardSummaryDTO();
        summary.setGeneratedAt(Timestamp.valueOf(LocalDateTime.now()));
        summary.setYear(year);

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (PACKAGE_STATUS status : PACKAGE_STATUS.values()) {
            statusCounts.put(status.name(), 0L);
        }
        packages.countPackagesGroupedByStatus().forEach(row -> {
            PACKAGE_STATUS status = (PACKAGE_STATUS) row[0];
            Number count = (Number) row[1];
            statusCounts.put(status.name(), count == null ? 0L : count.longValue());
        });
        summary.setStatusCounts(statusCounts);

        List<Long> monthlyShipmentCounts = new ArrayList<>(Collections.nCopies(12, 0L));
        packages.countPackagesByMonth(year).forEach(row -> {
            int month = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            if (month >= 1 && month <= 12) {
                monthlyShipmentCounts.set(month - 1, count);
            }
        });
        summary.setMonthlyShipmentCounts(monthlyShipmentCounts);

        List<Double> monthlyDeliveredRevenue = new ArrayList<>(Collections.nCopies(12, 0d));
        packages.sumDeliveredRevenueByMonth(year).forEach(row -> {
            int month = ((Number) row[0]).intValue();
            double amount = safeDouble(((Number) row[1]).doubleValue());
            if (month >= 1 && month <= 12) {
                monthlyDeliveredRevenue.set(month - 1, amount);
            }
        });
        summary.setMonthlyDeliveredRevenue(monthlyDeliveredRevenue);

        List<Long> recentPackageIds = packages.findRecentPackageIds(PageRequest.of(0, 12)).getContent();
        List<PackageDTO> recentPackages = loadRecentPackagesForDashboard(recentPackageIds);
        summary.setRecentPackages(recentPackages);
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "packagesAdminFinancialDashboard", key = "'singleton'", sync = true)
    public FinancialDashboardDTO loadAdminFinancialDashboard() {
        long startedAt = System.currentTimeMillis();
        Object[] settlementSummary = packageSettlements.loadFinancialSummary().stream()
                .findFirst()
                .orElse(new Object[]{0L, 0d, 0d, 0d, 0d, 0d});
        long settlementCount = ((Number) settlementSummary[0]).longValue();
        double customerRevenue = safeDouble(((Number) settlementSummary[1]).doubleValue());
        double serviceFees = safeDouble(((Number) settlementSummary[2]).doubleValue());
        double platformCommission = safeDouble(((Number) settlementSummary[3]).doubleValue());
        double averageOrderValue = safeDouble(((Number) settlementSummary[4]).doubleValue());
        double averageCourierPayout = safeDouble(((Number) settlementSummary[5]).doubleValue());
        long deliveredPackageCount = packageSettlements.countDeliveredSettlements();
        List<PackageSettlement> recentSettlements = packageSettlements.findRecentSettlementsWithPackage(
                PageRequest.of(0, FINANCIAL_RECENT_SETTLEMENT_LIMIT)
        );
        List<Object[]> payoutSummaryRows = courierPayouts.summarizeByStatus();
        List<Object[]> pendingPayoutSummaryRows = courierPayouts.summarizeByStatuses(
                OPEN_PAYOUT_STATUSES,
                PageRequest.of(0, FINANCIAL_PENDING_PAYOUT_LIMIT)
        );
        List<Object[]> trendRows = packageSettlements.loadSettlementTrendSummary(
                Timestamp.valueOf(YearMonth.now().minusMonths(FINANCIAL_TREND_MONTHS - 1).atDay(1).atStartOfDay())
        );
        String currency = packageSettlements.findRecentCurrencies(PageRequest.of(0, 1)).stream().findFirst().orElse("EUR");
        Object[] penaltySummary = courierPenalties.summarizeByStatus(COURIER_PENALTY_STATUS.ACTIVE).stream()
                .findFirst()
                .orElse(new Object[]{0L, 0d});
        long courierPenaltyCount = ((Number) penaltySummary[0]).longValue();
        double courierFinancialPenaltyAmount = safeDouble(((Number) penaltySummary[1]).doubleValue());
        long activeSuspensionCount = courierPenalties.countActiveSuspensions(
                COURIER_PENALTY_STATUS.ACTIVE,
                Timestamp.valueOf(LocalDateTime.now())
        );

        FinancialDashboardDTO dashboard = new FinancialDashboardDTO();
        dashboard.setGeneratedAt(Timestamp.valueOf(LocalDateTime.now()));
        dashboard.setCurrency(currency);
        dashboard.setSettlementCount(settlementCount);
        dashboard.setDeliveredPackageCount(deliveredPackageCount);
        double platformMargin = serviceFees + platformCommission;
        double takeRate = customerRevenue <= 0d ? 0d : platformMargin / customerRevenue;
        long pendingPayoutCount = 0L;
        long paidPayoutCount = 0L;
        double pendingPayoutAmount = 0d;
        double paidPayoutAmount = 0d;
        for (Object[] row : payoutSummaryRows) {
            COURIER_PAYOUT_STATUS status = row[0] == null ? null : (COURIER_PAYOUT_STATUS) row[0];
            long count = ((Number) row[1]).longValue();
            double amount = safeDouble(((Number) row[2]).doubleValue());
            if (status != null && OPEN_PAYOUT_STATUSES.contains(status)) {
                pendingPayoutCount += count;
                pendingPayoutAmount += amount;
            }
            if (COURIER_PAYOUT_STATUS.PAID.equals(status)) {
                paidPayoutCount += count;
                paidPayoutAmount += amount;
            }
        }

        dashboard.setCustomerRevenue(customerRevenue);
        dashboard.setPlatformServiceFees(serviceFees);
        dashboard.setPlatformCommissionAmount(platformCommission);
        dashboard.setPlatformMargin(platformMargin);
        dashboard.setAverageOrderValue(averageOrderValue);
        dashboard.setAverageCourierPayout(averageCourierPayout);
        dashboard.setPlatformTakeRate(takeRate);
        dashboard.setPendingPayoutCount(pendingPayoutCount);
        dashboard.setPaidPayoutCount(paidPayoutCount);
        dashboard.setCourierPayoutPendingAmount(pendingPayoutAmount);
        dashboard.setCourierPayoutPaidAmount(paidPayoutAmount);
        dashboard.setCourierPenaltyCount(courierPenaltyCount);
        dashboard.setCourierActiveSuspensionCount(activeSuspensionCount);
        dashboard.setCourierFinancialPenaltyAmount(courierFinancialPenaltyAmount);
        dashboard.setCustomerRevenueTrend(buildSettlementTrend(trendRows, 2));
        dashboard.setPlatformMarginTrend(buildSettlementTrend(trendRows, 3, 4));
        dashboard.setCourierPayoutTrend(buildSettlementTrend(trendRows, 5));
        dashboard.setRecentSettlements(buildRecentSettlements(recentSettlements));
        dashboard.setPendingPayouts(buildPendingPayoutSummaries(pendingPayoutSummaryRows, dashboard.getCurrency()));
        dashboard.setRecentCourierPenalties(buildCourierPenaltyDTOs(
                courierPenalties.findAllByOrderByCreatedAtDesc(PageRequest.of(0, FINANCIAL_PENDING_PAYOUT_LIMIT))
        ));
        logger.info("Built admin financial dashboard in {} ms with {} settlements and {} payout status groups",
                System.currentTimeMillis() - startedAt,
                settlementCount,
                payoutSummaryRows.size());
        return dashboard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierPenaltyDTO> loadAdminCourierPenalties(int limit) {
        int resolvedLimit = Math.max(1, Math.min(limit, 500));
        return buildCourierPenaltyDTOs(courierPenalties.findAllByOrderByCreatedAtDesc(PageRequest.of(0, resolvedLimit)));
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

        notifyCourierIfArrivedAtNextStop(deliveryPersonId, positionDTO);

        List<String> packageReferences = packages.findPackageReferencesInDeliveryByDeliveryPerson(deliveryPersonId);
        if (packageReferences.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, PositionDTO> updatedPositions = new HashMap<>();
        packageReferences.forEach(packageReference -> updatedPositions.putAll(updateTrackingPositionByPackageReference(packageReference, positionDTO)));
        return updatedPositions;
    }

    @Override
    @Transactional
    public Map<String, PositionDTO> updateTrackingPositionByPackageReference(String packageReference, PositionDTO positionDTO) {
        if (packageReference == null || packageReference.isBlank() || positionDTO == null
                || positionDTO.getLatitude() == null || positionDTO.getLongitude() == null) {
            return Collections.emptyMap();
        }

        int updatedRows = packages.updateTrackingPositionByPackageReference(
                packageReference,
                positionDTO.getLatitude(),
                positionDTO.getLongitude()
        );
        if (updatedRows <= 0) {
            return Collections.emptyMap();
        }

        trackingPositionCacheService.store(packageReference, positionDTO);
        Map<String, PositionDTO> updatedPositions = Map.of(packageReference, positionDTO);
        trackingBroadcastPublisher.broadcast("PACKAGE_SERVICE", updatedPositions);
        return updatedPositions;
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveTrackingPackageDTO getActiveTrackingPackage(Long deliveryPersonId) {
        if (deliveryPersonId == null) {
            return null;
        }
        return packages.findActiveTrackingPackagesByDeliveryPerson(deliveryPersonId).stream()
                .findFirst()
                .map(aPackage -> {
                    ActiveTrackingPackageDTO dto = new ActiveTrackingPackageDTO();
                    dto.setPackageReference(aPackage.getReference());
                    dto.setStatus(aPackage.getStatus() == null ? null : aPackage.getStatus().name());
                    return dto;
                })
                .orElse(null);
    }

    private List<FinancialTrendPointDTO> buildSettlementTrend(List<Object[]> trendRows, int... valueIndexes) {
        LinkedHashMap<YearMonth, Double> monthlyValues = new LinkedHashMap<>();
        YearMonth currentMonth = YearMonth.now();
        for (int monthOffset = FINANCIAL_TREND_MONTHS - 1; monthOffset >= 0; monthOffset--) {
            monthlyValues.put(currentMonth.minusMonths(monthOffset), 0d);
        }

        trendRows.forEach(row -> {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            YearMonth period = YearMonth.of(year, month);
            if (!monthlyValues.containsKey(period)) {
                return;
            }
            double total = 0d;
            for (int valueIndex : valueIndexes) {
                total += safeDouble(((Number) row[valueIndex]).doubleValue());
            }
            double aggregatedValue = total;
            monthlyValues.computeIfPresent(period, (key, value) -> value + aggregatedValue);
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

    private List<CourierPayoutSummaryDTO> buildPendingPayoutSummaries(List<Object[]> pendingPayoutSummaryRows, String fallbackCurrency) {
        return pendingPayoutSummaryRows.stream()
                .map(row -> {
                    CourierPayoutSummaryDTO item = new CourierPayoutSummaryDTO();
                    Long deliveryPersonId = row[0] == null ? null : ((Number) row[0]).longValue();
                    item.setDeliveryPersonId(deliveryPersonId);
                    item.setDeliveryPersonName(resolveDeliveryPersonName(
                            deliveryPersonId,
                            row[1] == null ? null : row[1].toString(),
                            row[2] == null ? null : row[2].toString(),
                            row[3] == null ? null : row[3].toString()
                    ));
                    item.setPackageCount(((Number) row[4]).longValue());
                    item.setAmount(roundMoney(((Number) row[5]).doubleValue()));
                    item.setOldestCreatedAt((Timestamp) row[6]);
                    item.setCurrency(fallbackCurrency);
                    return item;
                })
                .toList();
    }

    private String resolveDeliveryPersonName(User deliveryPerson) {
        if (deliveryPerson == null) {
            return messageSource.getMessage("courier.name.unassigned", null, Locale.FRANCE);
        }
        String fullName = Stream.of(deliveryPerson.getFirstName(), deliveryPerson.getLastName())
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" "));
        if (!fullName.isBlank()) {
            return fullName;
        }
        return deliveryPerson.getEmailAddress() == null || deliveryPerson.getEmailAddress().isBlank()
                ? messageSource.getMessage("courier.name.reference", new Object[]{deliveryPerson.getId()}, Locale.FRANCE)
                : deliveryPerson.getEmailAddress();
    }

    private String resolveDeliveryPersonName(Long deliveryPersonId, String firstName, String lastName, String emailAddress) {
        String fullName = Stream.of(firstName, lastName)
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" "));
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (emailAddress != null && !emailAddress.isBlank()) {
            return emailAddress;
        }
        return deliveryPersonId == null 
                ? messageSource.getMessage("courier.name.unassigned", null, Locale.FRANCE) 
                : messageSource.getMessage("courier.name.reference", new Object[]{deliveryPersonId}, Locale.FRANCE);
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
        courierPayout.setAmount(applyActiveFinancialPenalties(deliveryPerson, settlement.getCourierPayoutAmount()));
        courierPayout.setStatus(COURIER_PAYOUT_STATUS.PENDING);
        courierPayouts.save(courierPayout);
    }

    private PackageDTO toPackageDTO(Package aPackage) {
        Package normalizedPackage = normalizeSoftLockState(aPackage);
        PackageDTO packageDTO = new PackageDTO();
        packageDTO.setId(normalizedPackage.getId());
        packageDTO.setVersion(normalizedPackage.getVersion());
        packageDTO.setReference(normalizedPackage.getReference());
        packageDTO.setCreationDate(normalizedPackage.getCreationDate());
        packageDTO.setHeight(normalizedPackage.getHeight());
        packageDTO.setWidth(normalizedPackage.getWidth());
        packageDTO.setDepth(normalizedPackage.getDepth());
        packageDTO.setWeight(normalizedPackage.getWeight());
        packageDTO.setPackageSizeCategory(resolvePackageSizeCategory(normalizedPackage));
        packageDTO.setPictureURL(normalizedPackage.getPictureURL());
        packageDTO.setStatus(normalizedPackage.getStatus());
        packageDTO.setDeliveryPrice(normalizedPackage.getDeliveryPrice());
        PackageSettlement settlement = normalizedPackage.getPackageSettlement();
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
        packageDTO.setDeliverySpeed(normalizedPackage.getDeliverySpeed());
        packageDTO.setInsuranceSelected(normalizedPackage.getInsuranceSelected());
        packageDTO.setDeclaredValue(normalizedPackage.getDeclaredValue());
        packageDTO.setDistanceToDestination(normalizedPackage.getDistanceToDestination());
        packageDTO.setGuestMode(normalizedPackage.getGuestMode());
        packageDTO.setGuestAccessToken(normalizedPackage.getGuestAccessToken());
        packageDTO.setLastPositionLatitude(normalizedPackage.getLastPositionLatitude());
        packageDTO.setLastPositionLongitude(normalizedPackage.getLastPositionLongitude());
        packageDTO.setSenderID(normalizedPackage.getSender() == null ? null : normalizedPackage.getSender().getId());
        packageDTO.setIsSoftLockedBy(isSoftLockActive(normalizedPackage) ? normalizedPackage.getIsSoftLockedBy() : null);
        packageDTO.setSoftLockExpiresAt(isSoftLockActive(normalizedPackage) && normalizedPackage.getSoftLockExpiresAt() != null ? Timestamp.valueOf(normalizedPackage.getSoftLockExpiresAt()) : null);
        packageDTO.setAddresses(normalizedPackage.getAddresses() == null ? new ArrayList<>() : normalizedPackage.getAddresses().stream()
                .map(this::toAddressDTO)
                .toList());
        packageDTO.setPackageReservations(normalizedPackage.getPackageReservations() == null ? new ArrayList<>() : normalizedPackage.getPackageReservations().stream()
                .map(this::toPackageReservationDTO)
                .toList());
        packageDTO.setDocumentS(new LinkedHashMap<>());
        normalizedPackage.getDocument().stream()
                .filter(this::isPackageDocument)
                .forEach(document -> packageDTO.getDocumentS().put(document.getType(), toPackageDocumentMetadata(document)));
        return packageDTO;
    }

    private PackageDTO toSummaryPackageDTO(Package aPackage) {
        Package normalizedPackage = normalizeSoftLockState(aPackage);
        PackageDTO packageDTO = new PackageDTO();
        packageDTO.setId(normalizedPackage.getId());
        packageDTO.setVersion(normalizedPackage.getVersion());
        packageDTO.setReference(normalizedPackage.getReference());
        packageDTO.setCreationDate(normalizedPackage.getCreationDate());
        packageDTO.setHeight(normalizedPackage.getHeight());
        packageDTO.setWidth(normalizedPackage.getWidth());
        packageDTO.setDepth(normalizedPackage.getDepth());
        packageDTO.setWeight(normalizedPackage.getWeight());
        packageDTO.setPackageSizeCategory(resolvePackageSizeCategory(normalizedPackage));
        packageDTO.setPictureURL(normalizedPackage.getPictureURL());
        packageDTO.setStatus(normalizedPackage.getStatus());
        packageDTO.setDeliveryPrice(normalizedPackage.getDeliveryPrice());
        packageDTO.setDeliverySpeed(normalizedPackage.getDeliverySpeed());
        packageDTO.setInsuranceSelected(normalizedPackage.getInsuranceSelected());
        packageDTO.setDeclaredValue(normalizedPackage.getDeclaredValue());
        packageDTO.setSenderID(normalizedPackage.getSender() == null ? null : normalizedPackage.getSender().getId());
        packageDTO.setGuestMode(normalizedPackage.getGuestMode());
        packageDTO.setDistanceToDestination(normalizedPackage.getDistanceToDestination());
        packageDTO.setFromYou(null);
        packageDTO.setFiles(new ArrayList<>());
        packageDTO.setPackageReservations(new ArrayList<>());
        packageDTO.setDocumentS(new LinkedHashMap<>());
        packageDTO.setGuestAccessToken(null);
        packageDTO.setLastPositionLatitude(null);
        packageDTO.setLastPositionLongitude(null);
        packageDTO.setIsSoftLockedBy(isSoftLockActive(normalizedPackage) ? normalizedPackage.getIsSoftLockedBy() : null);
        packageDTO.setSoftLockExpiresAt(isSoftLockActive(normalizedPackage) && normalizedPackage.getSoftLockExpiresAt() != null ? Timestamp.valueOf(normalizedPackage.getSoftLockExpiresAt()) : null);

        PackageSettlement settlement = normalizedPackage.getPackageSettlement();
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

        packageDTO.setAddresses(normalizedPackage.getAddresses() == null ? new ArrayList<>() : normalizedPackage.getAddresses().stream()
                .map(this::toAddressDTO)
                .toList());
        return packageDTO;
    }

    private List<PackageDTO> loadRecentPackagesForDashboard(List<Long> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Package> packagesById = packages.findRecentPackagesWithDetailsByIds(packageIds).stream()
                .collect(Collectors.toMap(Package::getId, currentPackage -> currentPackage));
        return packageIds.stream()
                .map(packagesById::get)
                .filter(Objects::nonNull)
                .map(this::toSummaryPackageDTO)
                .toList();
    }

    private PackageDTO toNearbyPackageDTO(Package aPackage) {
        PackageDTO packageDTO = toSummaryPackageDTO(aPackage);
        List<AddressDTO> lightweightAddresses = packageDTO.getAddresses() == null
                ? List.of()
                : packageDTO.getAddresses().stream()
                .map(this::toNearbyAddressDTO)
                .toList();
        packageDTO.setAddresses(lightweightAddresses);
        return packageDTO;
    }

    private AddressDTO toNearbyAddressDTO(AddressDTO source) {
        AddressDTO addressDTO = new AddressDTO();
        addressDTO.setId(source.getId());
        addressDTO.setType(source.getType());
        addressDTO.setFirstName(source.getFirstName());
        addressDTO.setLastName(source.getLastName());
        addressDTO.setLine1(source.getLine1());
        addressDTO.setTown(source.getTown());
        addressDTO.setZipCode(source.getZipCode());
        addressDTO.setCountry(source.getCountry());
        addressDTO.setLatitude(source.getLatitude());
        addressDTO.setLongitude(source.getLongitude());
        addressDTO.setAddressAuto(source.getAddressAuto());
        return addressDTO;
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
        reservationDTO.setDeliveryPersonId(reservation.getDeliveryPerson() == null ? null : reservation.getDeliveryPerson().getId());
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
        Package aPackage = packages.findByIdForUpdate(packageID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found"));
        User user = users.findById(deliveryPersonID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery user not found"));

        enforceCourierNotSuspended(deliveryPersonID);

        if (deliveryRoutes.existsByDeliveryPersonAndStatuses(deliveryPersonID, ACTIVE_DELIVERY_ROUTE_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An active delivery route already blocks new reservations");
        }

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

        if (isSoftLockActive(aPackage) && aPackage.getIsSoftLockedBy() != null
                && !Objects.equals(aPackage.getIsSoftLockedBy(), String.valueOf(deliveryPersonID))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Package is temporarily consulted by another courier");
        }

        if (!isPackageCompatibleWithVehicleType(aPackage, user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "errorPackageNotCompatibleWithVehicle");
        }

        enforceCourierCapacity(user);
        clearSoftLock(aPackage);
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
        packageDocumentStorageService.deleteDirectory(packageDirectory);
    }

    private void primePackageForAsyncNotifications(Package aPackage) {
        if (aPackage == null) {
            return;
        }

        if (aPackage.getAddresses() != null) {
            aPackage.getAddresses().forEach(address -> {
                address.getType();
                address.getEmail();
            });
        }

        if (aPackage.getPackageReservations() != null) {
            aPackage.getPackageReservations().forEach(reservation -> {
                reservation.getStatus();
                reservation.getPickUpOTP();
                reservation.getDeliveryOTP();
                if (reservation.getDeliveryPerson() != null) {
                    reservation.getDeliveryPerson().getEmailAddress();
                    reservation.getDeliveryPerson().getFirstName();
                    reservation.getDeliveryPerson().getLastName();
                }
            });
        }
    }

    private void cleanupTempArtifacts(Path tempDir) {
        if (tempDir == null) {
            return;
        }
        try (var pathStream = Files.walk(tempDir)) {
            pathStream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (DirectoryNotEmptyException ignored) {
                } catch (IOException exception) {
                    logger.warn("Unable to delete temp artifact {}: {}", path, exception.getMessage());
                }
            });
        } catch (IOException exception) {
            logger.warn("Unable to cleanup package temp artifacts {}: {}", tempDir, exception.getMessage());
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

    private List<PackageDTO> loadNearbyNewPackages(double centerLat, double centerLng, double rayonEnMetres) {
        return loadNearbyNewPackages(centerLat, centerLng, rayonEnMetres, null);
    }

    private List<PackageDTO> loadNearbyNewPackages(double centerLat, double centerLng, double rayonEnMetres, String deliveryMode) {
        double latitudeDelta = metersToLatitudeDelta(rayonEnMetres);
        double longitudeDelta = metersToLongitudeDelta(rayonEnMetres, centerLat);
        List<Long> nearbyPackageIds = packages.findNearbyNewPackageIds(
                centerLat,
                centerLng,
                rayonEnMetres,
                centerLat - latitudeDelta,
                centerLat + latitudeDelta,
                centerLng - longitudeDelta,
                centerLng + longitudeDelta,
                Math.max(1, maxNearbyResults)
        );
        return loadNearbyPackagesByIds(centerLat, centerLng, rayonEnMetres, nearbyPackageIds, deliveryMode);
    }

    private List<PackageDTO> loadNearbyPackagesByIds(double centerLat, double centerLng, double rayonEnMetres, List<Long> nearbyPackageIds) {
        return loadNearbyPackagesByIds(centerLat, centerLng, rayonEnMetres, nearbyPackageIds, null);
    }

    private List<PackageDTO> loadNearbyPackagesByIds(double centerLat, double centerLng, double rayonEnMetres, List<Long> nearbyPackageIds, String deliveryMode) {
        if (nearbyPackageIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> packageOrder = new HashMap<>();
        for (int index = 0; index < nearbyPackageIds.size(); index += 1) {
            packageOrder.put(nearbyPackageIds.get(index), index);
        }

        return packages.findRecentPackagesWithDetailsByIds(nearbyPackageIds).stream()
                .filter(Objects::nonNull)
                .filter(aPackage -> PACKAGE_STATUS.NEW.equals(aPackage.getStatus()))
                .filter(aPackage -> deliveryMode == null || isPackageCompatibleWithMode(aPackage, deliveryMode))
                .map(aPackage -> {
                    Address departureAddress = getDepartureAddress(aPackage.getAddresses());
                    if (departureAddress == null
                            || departureAddress.getLatitude() == null
                            || departureAddress.getLongitude() == null) {
                        return null;
                    }
                    double distanceMeters = haversineMeters(
                            departureAddress.getLatitude().doubleValue(),
                            departureAddress.getLongitude().doubleValue(),
                            centerLat,
                            centerLng
                    );
                    if (distanceMeters > rayonEnMetres) {
                        return null;
                    }
                    PackageDTO packageDTO = toNearbyPackageDTO(aPackage);
                    packageDTO.setFromYou(formatApproximateDistance(distanceMeters));
                    return packageDTO;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(packageDTO -> packageOrder.getOrDefault(packageDTO.getId(), Integer.MAX_VALUE)))
                .toList();
    }

    public static String buildNearbyCacheKey(String latitude, String longitude, double rayonEnMetres) {
        return String.format("%s:%s:%s",
                normalizeNearbyCoordinateKey(latitude),
                normalizeNearbyCoordinateKey(longitude),
                normalizeNearbyRadiusKey(rayonEnMetres));
    }

    public static String normalizeNearbyCoordinateKey(String value) {
        return BigDecimal.valueOf(Double.parseDouble(value))
                .setScale(4, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static String normalizeNearbyRadiusKey(double rayonEnMetres) {
        return BigDecimal.valueOf(rayonEnMetres)
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private double resolveDepartureDistanceMeters(PackageDTO packageDTO, double centerLat, double centerLng) {
        AddressDTO departureAddress = getDepartureAddress(packageDTO.getAddresses());
        if (departureAddress == null
                || departureAddress.getLatitude() == null
                || departureAddress.getLongitude() == null) {
            return Double.MAX_VALUE;
        }
        return haversineMeters(
                departureAddress.getLatitude().doubleValue(),
                departureAddress.getLongitude().doubleValue(),
                centerLat,
                centerLng
        );
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

    private String normalizeRoutePlanMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "personalRoute";
        }
        return "directAddress".equalsIgnoreCase(mode.trim()) ? "directAddress" : "personalRoute";
    }

    private GeoPoint toGeoPoint(RoutePlanPointDTO point) {
        if (point == null || point.getLat() == null || point.getLng() == null) {
            return null;
        }
        return new GeoPoint(point.getLat(), point.getLng());
    }

    private RoutePlanPointDTO toRoutePlanPoint(GeoPoint point) {
        RoutePlanPointDTO dto = new RoutePlanPointDTO();
        dto.setLat(point.lat);
        dto.setLng(point.lng);
        return dto;
    }

    private PlannablePackage toPlannablePackage(Package pkg) {
        Address departure = getDepartureAddress(pkg.getAddresses());
        Address arrival = getArrivalAddress(pkg.getAddresses());
        if (departure == null || arrival == null
                || departure.getLatitude() == null || departure.getLongitude() == null
                || arrival.getLatitude() == null || arrival.getLongitude() == null) {
            return null;
        }
        PackageDTO dto = toSummaryPackageDTO(pkg);
        return new PlannablePackage(
                pkg.getId(),
                dto.getReference(),
                buildAddressLabel(departure),
                buildAddressLabel(arrival),
                new GeoPoint(departure.getLatitude().doubleValue(), departure.getLongitude().doubleValue()),
                new GeoPoint(arrival.getLatitude().doubleValue(), arrival.getLongitude().doubleValue()),
                resolveDisplayedRouteAmount(dto),
                resolvePackageWeightKg(dto),
                resolvePackageVolumeCm3(dto),
                dto
        );
    }

    private String buildAddressLabel(Address address) {
        if (address == null) {
            return "";
        }
        return Stream.of(address.getLine1(), address.getLine2(), address.getZipCode(), address.getTown(), address.getCountry())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" "));
    }

    private double resolveDisplayedRouteAmount(PackageDTO dto) {
        if (dto == null) {
            return 0d;
        }
        if (dto.getCourierPayoutAmount() != null) {
            return dto.getCourierPayoutAmount();
        }
        if (dto.getCustomerTotalPrice() != null) {
            return dto.getCustomerTotalPrice();
        }
        if (dto.getDeliveryPrice() != null) {
            return dto.getDeliveryPrice();
        }
        return 0d;
    }

    private double resolvePackageWeightKg(PackageDTO dto) {
        return dto != null && dto.getWeight() != null ? dto.getWeight() : 0d;
    }

    private double resolvePackageVolumeCm3(PackageDTO dto) {
        if (dto == null) {
            return 0d;
        }
        double height = dto.getHeight() != null ? dto.getHeight() : 0d;
        double width = dto.getWidth() != null ? dto.getWidth() : 0d;
        double depth = dto.getDepth() != null ? dto.getDepth() : 0d;
        return height * width * depth;
    }

    private List<PlannablePackage> constrainPackagesForPersonalRoute(List<PlannablePackage> plannablePackages,
                                                                     GeoPoint start,
                                                                     GeoPoint end,
                                                                     RoutePlanRequestDTO request) {
        Comparator<PlannablePackage> byPickupProgress = Comparator
                .comparingDouble((PlannablePackage pkg) ->
                        clampProgress(projectionProgress(pkg.pickupPoint(), start, end)))
                .thenComparing(PlannablePackage::packageId);
        List<PlannablePackage> orderedPackages = plannablePackages.stream()
                .sorted(byPickupProgress)
                .collect(Collectors.toList());
        return constrainPackagesByCapacity(orderedPackages, request);
    }

    private List<PlannablePackage> constrainPackagesForDirectRoute(List<PlannablePackage> plannablePackages,
                                                                   GeoPoint start,
                                                                   GeoPoint end,
                                                                   RoutePlanRequestDTO request) {
        List<RouteStopCandidate> pickupStops = plannablePackages.stream()
                .map(plannablePackage -> new RouteStopCandidate("pickup", plannablePackage, plannablePackage.pickupPoint(), 0d, 0))
                .collect(Collectors.toList());
        List<RouteStopCandidate> orderedStops = orderStopsByNearestNeighbor(start, pickupStops, end);
        List<PlannablePackage> orderedPackages = orderedStops.stream()
                .map(RouteStopCandidate::plannablePackage)
                .distinct()
                .collect(Collectors.toList());
        return constrainPackagesByCapacity(orderedPackages, request);
    }

    private List<PlannablePackage> constrainPackagesByCapacity(List<PlannablePackage> orderedPackages,
                                                               RoutePlanRequestDTO request) {
        int maxPackages = Math.max(1, resolveMaxPackagesForSearch(request.getDeliveryMode(), request.getVehicleType()));
        double maxWeightKg = resolveMaxWeightKg(request.getDeliveryMode(), request.getVehicleType());
        double maxVolumeCm3 = resolveMaxVolumeCm3(request.getDeliveryMode(), request.getVehicleType());
        List<PlannablePackage> acceptedPackages = new ArrayList<>();
        double totalWeightKg = 0d;
        double totalVolumeCm3 = 0d;
        for (PlannablePackage candidate : orderedPackages) {
            if (acceptedPackages.size() >= maxPackages) {
                break;
            }
            double nextWeightKg = totalWeightKg + candidate.weightKg();
            double nextVolumeCm3 = totalVolumeCm3 + candidate.volumeCm3();
            if (nextWeightKg > maxWeightKg || nextVolumeCm3 > maxVolumeCm3) {
                continue;
            }
            acceptedPackages.add(candidate);
            totalWeightKg = nextWeightKg;
            totalVolumeCm3 = nextVolumeCm3;
        }
        return acceptedPackages;
    }

    private RoutePlanDTO buildPersonalRoutePlanDto(List<PlannablePackage> packagesToPlan,
                                                   GeoPoint start,
                                                   GeoPoint end,
                                                   RoutePlanRequestDTO request) {
        List<RouteStopCandidate> orderedStops = packagesToPlan.stream()
                .flatMap(plannablePackage -> {
                    double pickupProgress = clampProgress(projectionProgress(plannablePackage.pickupPoint(), start, end));
                    double rawDropoffProgress = clampProgress(projectionProgress(plannablePackage.dropoffPoint(), start, end));
                    double dropoffProgress = Math.max(rawDropoffProgress, pickupProgress + 0.0001d);
                    return Stream.of(
                            new RouteStopCandidate("pickup", plannablePackage, plannablePackage.pickupPoint(), pickupProgress, 0),
                            new RouteStopCandidate("dropoff", plannablePackage, plannablePackage.dropoffPoint(), dropoffProgress, 1)
                    );
                })
                .sorted(Comparator
                        .comparingDouble(RouteStopCandidate::routeProgress)
                        .thenComparing(RouteStopCandidate::typePriority)
                        .thenComparing(candidate -> candidate.plannablePackage().packageId()))
                .collect(Collectors.toList());
        return finalizeRoutePlan("personalRoute", request.getSelectedPackageId(), packagesToPlan, start, end, orderedStops);
    }

    private RoutePlanDTO buildDirectRoutePlanDto(List<PlannablePackage> packagesToPlan,
                                                 GeoPoint start,
                                                 GeoPoint end,
                                                 RoutePlanRequestDTO request) {
        List<RouteStopCandidate> pickupStops = packagesToPlan.stream()
                .map(plannablePackage -> new RouteStopCandidate("pickup", plannablePackage, plannablePackage.pickupPoint(), 0d, 0))
                .collect(Collectors.toList());
        List<RouteStopCandidate> dropoffStops = packagesToPlan.stream()
                .map(plannablePackage -> new RouteStopCandidate("dropoff", plannablePackage, plannablePackage.dropoffPoint(), 0d, 1))
                .collect(Collectors.toList());

        List<RouteStopCandidate> orderedPickups = optimizeStopsWithTwoOpt(
                start,
                orderStopsByNearestNeighbor(start, pickupStops, null),
                end,
                2
        );
        GeoPoint pickupEnd = orderedPickups.isEmpty() ? start : orderedPickups.get(orderedPickups.size() - 1).point();
        List<RouteStopCandidate> orderedDropoffs = optimizeStopsWithTwoOpt(
                pickupEnd,
                orderStopsByNearestNeighbor(pickupEnd, dropoffStops, end),
                end,
                2
        );
        List<RouteStopCandidate> orderedStops = new ArrayList<>(orderedPickups.size() + orderedDropoffs.size());
        orderedStops.addAll(orderedPickups);
        orderedStops.addAll(orderedDropoffs);
        return finalizeRoutePlan("directAddress", request.getSelectedPackageId(), packagesToPlan, start, end, orderedStops);
    }

    private double clampProgress(double progress) {
        if (Double.isNaN(progress) || Double.isInfinite(progress)) {
            return 0d;
        }
        return Math.max(0d, Math.min(1d, progress));
    }

    private List<RouteStopCandidate> orderStopsByNearestNeighbor(GeoPoint initialPoint,
                                                                 List<RouteStopCandidate> stops,
                                                                 GeoPoint finalBiasPoint) {
        List<RouteStopCandidate> pendingStops = new ArrayList<>(stops);
        List<RouteStopCandidate> orderedStops = new ArrayList<>(stops.size());
        GeoPoint currentPoint = initialPoint;
        while (!pendingStops.isEmpty()) {
            int bestIndex = 0;
            double bestScore = Double.MAX_VALUE;
            for (int index = 0; index < pendingStops.size(); index++) {
                RouteStopCandidate stop = pendingStops.get(index);
                double distanceFromCurrent = haversineMeters(currentPoint.lat, currentPoint.lng, stop.point().lat, stop.point().lng);
                double distanceToFinalBias = finalBiasPoint == null ? 0d : haversineMeters(stop.point().lat, stop.point().lng, finalBiasPoint.lat, finalBiasPoint.lng);
                double score = distanceFromCurrent + (distanceToFinalBias * 0.1d);
                if (score < bestScore) {
                    bestScore = score;
                    bestIndex = index;
                }
            }
            RouteStopCandidate selectedStop = pendingStops.remove(bestIndex);
            orderedStops.add(selectedStop);
            currentPoint = selectedStop.point();
        }
        return orderedStops;
    }

    private List<RouteStopCandidate> optimizeStopsWithTwoOpt(GeoPoint start,
                                                             List<RouteStopCandidate> stops,
                                                             GeoPoint end,
                                                             int maxPasses) {
        if (stops == null || stops.size() < 4) {
            return stops == null ? List.of() : stops;
        }
        List<RouteStopCandidate> optimized = new ArrayList<>(stops);
        double bestDistance = computePathDistance(start, optimized, end);
        int pass = 0;
        boolean improved = true;
        while (improved && pass < maxPasses) {
            improved = false;
            pass += 1;
            for (int i = 0; i < optimized.size() - 2; i += 1) {
                for (int j = i + 1; j < optimized.size() - 1; j += 1) {
                    List<RouteStopCandidate> candidate = new ArrayList<>(optimized.size());
                    candidate.addAll(optimized.subList(0, i));
                    List<RouteStopCandidate> reversed = new ArrayList<>(optimized.subList(i, j + 1));
                    Collections.reverse(reversed);
                    candidate.addAll(reversed);
                    candidate.addAll(optimized.subList(j + 1, optimized.size()));
                    double candidateDistance = computePathDistance(start, candidate, end);
                    if (candidateDistance + 1d < bestDistance) {
                        optimized = candidate;
                        bestDistance = candidateDistance;
                        improved = true;
                    }
                }
            }
        }
        return optimized;
    }

    private double computePathDistance(GeoPoint start, List<RouteStopCandidate> stops, GeoPoint end) {
        GeoPoint current = start;
        double totalMeters = 0d;
        if (stops != null) {
            for (RouteStopCandidate stop : stops) {
                totalMeters += haversineMeters(current.lat, current.lng, stop.point().lat, stop.point().lng);
                current = stop.point();
            }
        }
        totalMeters += haversineMeters(current.lat, current.lng, end.lat, end.lng);
        return totalMeters;
    }

    private RoutePlanDTO finalizeRoutePlan(String mode,
                                           Long selectedPackageId,
                                           List<PlannablePackage> packagesToPlan,
                                           GeoPoint start,
                                           GeoPoint end,
                                           List<RouteStopCandidate> orderedStops) {
        RoutePlanDTO routePlan = new RoutePlanDTO();
        routePlan.setMode(mode);
        routePlan.setSelectedPackageId(selectedPackageId);
        routePlan.setStart(toRoutePlanPoint(start));
        routePlan.setEnd(toRoutePlanPoint(end));
        routePlan.setPackageIds(packagesToPlan.stream().map(PlannablePackage::packageId).collect(Collectors.toList()));

        List<RoutePlanStopDTO> stops = new ArrayList<>(orderedStops.size());
        Map<Long, RoutePlanPackageAnnotationDTO> annotations = new LinkedHashMap<>();
        for (int index = 0; index < orderedStops.size(); index++) {
            RouteStopCandidate stopCandidate = orderedStops.get(index);
            int order = index + 1;
            RoutePlanStopDTO stop = new RoutePlanStopDTO();
            stop.setKind(stopCandidate.kind());
            stop.setPackageId(stopCandidate.plannablePackage().packageId());
            stop.setPackageReference(stopCandidate.plannablePackage().packageReference());
            stop.setAddressLabel("pickup".equals(stopCandidate.kind())
                    ? stopCandidate.plannablePackage().pickupLabel()
                    : stopCandidate.plannablePackage().dropoffLabel());
            stop.setLat(stopCandidate.point().lat);
            stop.setLng(stopCandidate.point().lng);
            stop.setOrder(order);
            stops.add(stop);

            RoutePlanPackageAnnotationDTO annotation = annotations.computeIfAbsent(stop.getPackageId(), ignored -> {
                RoutePlanPackageAnnotationDTO value = new RoutePlanPackageAnnotationDTO();
                value.setSortOrder(order);
                return value;
            });
            if ("pickup".equals(stopCandidate.kind())) {
                annotation.setPickupOrder(order);
                annotation.setSortOrder(Math.min(annotation.getSortOrder(), order));
            } else {
                annotation.setDropoffOrder(order);
            }
            int stopCount = (annotation.getPickupOrder() != null ? 1 : 0) + (annotation.getDropoffOrder() != null ? 1 : 0);
            annotation.setStopCount(stopCount);
        }

        routePlan.setStops(stops);
        routePlan.setPackageAnnotations(annotations);
        routePlan.setMetrics(computeRoutePlanMetrics(start, end, orderedStops, packagesToPlan));
        routePlan.setStatus(DELIVERY_ROUTE_STATUS.PLANNED.name());
        List<String> googleMapsNavigationUrls = buildGoogleMapsNavigationUrls(start, end, stops);
        routePlan.setGoogleMapsNavigationUrls(googleMapsNavigationUrls);
        routePlan.setGoogleMapsNavigationUrl(googleMapsNavigationUrls.isEmpty() ? null : googleMapsNavigationUrls.get(0));
        return routePlan;
    }

    private RoutePlanMetricsDTO computeRoutePlanMetrics(GeoPoint start,
                                                        GeoPoint end,
                                                        List<RouteStopCandidate> orderedStops,
                                                        List<PlannablePackage> packagesToPlan) {
        double totalDistanceMeters = computePathDistance(start, orderedStops, end);
        double directDistanceMeters = haversineMeters(start.lat, start.lng, end.lat, end.lng);
        double detourMeters = Math.max(0d, totalDistanceMeters - directDistanceMeters);
        double detourRatio = directDistanceMeters > 0d ? totalDistanceMeters / directDistanceMeters : 1d;
        double totalDisplayedAmount = packagesToPlan.stream().mapToDouble(PlannablePackage::displayedAmount).sum();
        double totalWeightKg = packagesToPlan.stream().mapToDouble(PlannablePackage::weightKg).sum();
        double totalVolumeCm3 = packagesToPlan.stream().mapToDouble(PlannablePackage::volumeCm3).sum();
        double estimatedDurationMinutes = ((totalDistanceMeters / 1000d) / 35d) * 60d;
        double payoutPerKm = totalDistanceMeters > 0d ? totalDisplayedAmount / (totalDistanceMeters / 1000d) : 0d;
        double payoutPerHour = estimatedDurationMinutes > 0d ? totalDisplayedAmount / (estimatedDurationMinutes / 60d) : 0d;
        double qualityScore = Math.max(0d, roundTo(2, payoutPerKm * 10d - Math.max(0d, detourRatio - 1d) * 12d));

        RoutePlanMetricsDTO metrics = new RoutePlanMetricsDTO();
        metrics.setTotalDistanceMeters(roundTo(2, totalDistanceMeters));
        metrics.setDirectDistanceMeters(roundTo(2, directDistanceMeters));
        metrics.setDetourMeters(roundTo(2, detourMeters));
        metrics.setDetourRatio(roundTo(4, detourRatio));
        metrics.setEstimatedDurationMinutes(roundTo(1, estimatedDurationMinutes));
        metrics.setTotalDisplayedAmount(roundTo(2, totalDisplayedAmount));
        metrics.setTotalWeightKg(roundTo(2, totalWeightKg));
        metrics.setTotalVolumeCm3(roundTo(2, totalVolumeCm3));
        metrics.setPayoutPerKm(roundTo(2, payoutPerKm));
        metrics.setPayoutPerHour(roundTo(2, payoutPerHour));
        metrics.setQualityScore(qualityScore);
        return metrics;
    }

    private RoutePlanDTO filterRoutePlanToReserved(RoutePlanDTO routePlan, List<Long> reservedPackageIds) {
        if (routePlan == null || reservedPackageIds == null || reservedPackageIds.isEmpty()) {
            return null;
        }
        Set<Long> reservedSet = new LinkedHashSet<>(reservedPackageIds);
        RoutePlanDTO filtered = new RoutePlanDTO();
        filtered.setMode(routePlan.getMode());
        filtered.setSelectedPackageId(reservedSet.contains(routePlan.getSelectedPackageId()) ? routePlan.getSelectedPackageId() : reservedPackageIds.get(0));
        filtered.setStart(routePlan.getStart());
        filtered.setEnd(routePlan.getEnd());
        filtered.setPackageIds(routePlan.getPackageIds().stream()
                .filter(reservedSet::contains)
                .collect(Collectors.toList()));

        List<RoutePlanStopDTO> stops = routePlan.getStops().stream()
                .filter(stop -> reservedSet.contains(stop.getPackageId()))
                .map(this::copyRoutePlanStop)
                .collect(Collectors.toList());
        for (int index = 0; index < stops.size(); index++) {
            stops.get(index).setOrder(index + 1);
        }
        filtered.setStops(stops);

        Map<Long, RoutePlanPackageAnnotationDTO> annotations = new LinkedHashMap<>();
        for (RoutePlanStopDTO stop : stops) {
            RoutePlanPackageAnnotationDTO annotation = annotations.computeIfAbsent(stop.getPackageId(), ignored -> {
                RoutePlanPackageAnnotationDTO value = new RoutePlanPackageAnnotationDTO();
                value.setSortOrder(stop.getOrder());
                return value;
            });
            if ("pickup".equals(stop.getKind())) {
                annotation.setPickupOrder(stop.getOrder());
                annotation.setSortOrder(Math.min(annotation.getSortOrder(), stop.getOrder()));
            } else {
                annotation.setDropoffOrder(stop.getOrder());
            }
            annotation.setStopCount((annotation.getPickupOrder() != null ? 1 : 0) + (annotation.getDropoffOrder() != null ? 1 : 0));
        }
        filtered.setPackageAnnotations(annotations);

        List<Long> plannedPackageIds = reservedPackageIds.stream()
                .filter(packageId -> routePlan.getPackageAnnotations().containsKey(packageId))
                .collect(Collectors.toList());
        Map<Long, Package> plannedPackagesById = packages.findPackagesWithAddressesByIds(plannedPackageIds).stream()
                .collect(Collectors.toMap(Package::getId, pkg -> pkg));
        List<PlannablePackage> plannablePackages = plannedPackageIds.stream()
                .map(plannedPackagesById::get)
                .filter(Objects::nonNull)
                .map(this::normalizeSoftLockState)
                .filter(Objects::nonNull)
                .map(this::toPlannablePackage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        filtered.setMetrics(computeRoutePlanMetrics(
                toGeoPoint(filtered.getStart()),
                toGeoPoint(filtered.getEnd()),
                stops.stream()
                        .map(stop -> {
                            PlannablePackage pkg = plannablePackages.stream()
                                    .filter(candidate -> Objects.equals(candidate.packageId(), stop.getPackageId()))
                                    .findFirst()
                                    .orElse(null);
                            GeoPoint point = pkg == null
                                    ? new GeoPoint(stop.getLat(), stop.getLng())
                                    : ("pickup".equals(stop.getKind()) ? pkg.pickupPoint() : pkg.dropoffPoint());
                            return new RouteStopCandidate(stop.getKind(), pkg, point, 0d, "pickup".equals(stop.getKind()) ? 0 : 1);
                        })
                        .collect(Collectors.toList()),
                plannablePackages
        ));
        return filtered;
    }

    private RoutePlanStopDTO copyRoutePlanStop(RoutePlanStopDTO source) {
        RoutePlanStopDTO copy = new RoutePlanStopDTO();
        copy.setKind(source.getKind());
        copy.setPackageId(source.getPackageId());
        copy.setPackageReference(source.getPackageReference());
        copy.setAddressLabel(source.getAddressLabel());
        copy.setLat(source.getLat());
        copy.setLng(source.getLng());
        copy.setOrder(source.getOrder());
        return copy;
    }

    private List<String> buildGoogleMapsNavigationUrls(GeoPoint start, GeoPoint end, List<RoutePlanStopDTO> stops) {
        List<String> urls = new ArrayList<>();
        if (start == null || end == null) {
            return urls;
        }
        List<GeoPoint> points = new ArrayList<>();
        points.add(start);
        if (stops != null) {
            for (RoutePlanStopDTO stop : stops) {
                if (stop.getLat() != null && stop.getLng() != null) {
                    points.add(new GeoPoint(stop.getLat(), stop.getLng()));
                }
            }
        }
        points.add(end);
        if (points.size() < 2) {
            return urls;
        }

        final int maxIntermediateWaypoints = 8;
        int segmentStartIndex = 0;
        while (segmentStartIndex < points.size() - 1) {
            int segmentEndIndex = Math.min(points.size() - 1, segmentStartIndex + maxIntermediateWaypoints + 1);
            GeoPoint origin = points.get(segmentStartIndex);
            GeoPoint destination = points.get(segmentEndIndex);
            StringBuilder url = new StringBuilder("https://www.google.com/maps/dir/?api=1");
            url.append("&travelmode=driving");
            url.append("&origin=").append(origin.lat).append(",").append(origin.lng);
            url.append("&destination=").append(destination.lat).append(",").append(destination.lng);
            if (segmentEndIndex - segmentStartIndex > 1) {
                String waypoints = IntStream.range(segmentStartIndex + 1, segmentEndIndex)
                        .mapToObj(index -> points.get(index).lat + "," + points.get(index).lng)
                        .collect(Collectors.joining("|"));
                if (!waypoints.isBlank()) {
                    url.append("&waypoints=").append(waypoints);
                }
            }
            urls.add(url.toString());
            segmentStartIndex = segmentEndIndex;
        }
        return urls;
    }

    private double resolveMaxWeightKg(String deliveryMode, String vehicleType) {
        String normalizedVehicleType = vehicleType == null ? "" : vehicleType.trim().toUpperCase(Locale.ROOT);
        return switch (normalizedVehicleType) {
            case "ON_FOOT", "FOOT" -> 8d;
            case "BIKE", "BICYCLE" -> 15d;
            case "SCOOTER", "MOTORBIKE", "MOTORCYCLE" -> 30d;
            case "CAR" -> 250d;
            case "SMALL_TRUCK" -> 800d;
            case "VAN", "LARGE_VAN" -> 1200d;
            case "TRUCK" -> 5000d;
            case "HEAVY", "SEMI_TRAILER" -> 20000d;
            default -> switch (deliveryMode == null ? "" : deliveryMode.trim().toUpperCase(Locale.ROOT)) {
                case "ON_FOOT" -> 8d;
                case "BIKE" -> 15d;
                case "SCOOTER" -> 30d;
                case "CAR" -> 250d;
                case "VAN" -> 1200d;
                case "TRUCK" -> 5000d;
                case "HEAVY" -> 20000d;
                default -> 250d;
            };
        };
    }

    private double resolveMaxVolumeCm3(String deliveryMode, String vehicleType) {
        String normalizedVehicleType = vehicleType == null ? "" : vehicleType.trim().toUpperCase(Locale.ROOT);
        return switch (normalizedVehicleType) {
            case "ON_FOOT", "FOOT" -> 12000d;
            case "BIKE", "BICYCLE" -> 40000d;
            case "SCOOTER", "MOTORBIKE", "MOTORCYCLE" -> 90000d;
            case "CAR" -> 450000d;
            case "SMALL_TRUCK" -> 2000000d;
            case "VAN", "LARGE_VAN" -> 5000000d;
            case "TRUCK" -> 15000000d;
            case "HEAVY", "SEMI_TRAILER" -> 60000000d;
            default -> switch (deliveryMode == null ? "" : deliveryMode.trim().toUpperCase(Locale.ROOT)) {
                case "ON_FOOT" -> 12000d;
                case "BIKE" -> 40000d;
                case "SCOOTER" -> 90000d;
                case "CAR" -> 450000d;
                case "VAN" -> 5000000d;
                case "TRUCK" -> 15000000d;
                case "HEAVY" -> 60000000d;
                default -> 450000d;
            };
        };
    }

    private DeliveryRoute createDeliveryRoute(Long deliveryPersonID, RoutePlanDTO routePlan) {
        User deliveryPerson = users.findById(deliveryPersonID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery user not found"));
        DeliveryRoute deliveryRoute = new DeliveryRoute();
        deliveryRoute.setDeliveryPerson(deliveryPerson);
        deliveryRoute.setStatus(DELIVERY_ROUTE_STATUS.PLANNED);
        deliveryRoute.setStartLatitude(routePlan.getStart() == null ? null : routePlan.getStart().getLat());
        deliveryRoute.setStartLongitude(routePlan.getStart() == null ? null : routePlan.getStart().getLng());
        deliveryRoute.setEndLatitude(routePlan.getEnd() == null ? null : routePlan.getEnd().getLat());
        deliveryRoute.setEndLongitude(routePlan.getEnd() == null ? null : routePlan.getEnd().getLng());
        deliveryRoute.setTotalDistanceMeters(routePlan.getMetrics() == null ? null : routePlan.getMetrics().getTotalDistanceMeters());
        deliveryRoute.setEstimatedDurationMinutes(routePlan.getMetrics() == null ? null : routePlan.getMetrics().getEstimatedDurationMinutes());
        deliveryRoute.setTotalCourierPayout(routePlan.getMetrics() == null ? null : routePlan.getMetrics().getTotalDisplayedAmount());
        deliveryRoute.setTotalWeightKg(routePlan.getMetrics() == null ? null : routePlan.getMetrics().getTotalWeightKg());
        deliveryRoute.setTotalVolumeCm3(routePlan.getMetrics() == null ? null : routePlan.getMetrics().getTotalVolumeCm3());
        deliveryRoute.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));

        Map<Long, Package> routePackages = packages.findAllById(routePlan.getPackageIds()).stream()
                .collect(Collectors.toMap(Package::getId, pkg -> pkg));
        List<DeliveryRouteStop> deliveryRouteStops = new ArrayList<>();
        for (RoutePlanStopDTO stop : routePlan.getStops()) {
            Package pkg = routePackages.get(stop.getPackageId());
            if (pkg == null) {
                continue;
            }
            DeliveryRouteStop deliveryRouteStop = new DeliveryRouteStop();
            deliveryRouteStop.setDeliveryRoute(deliveryRoute);
            deliveryRouteStop.setaPackage(pkg);
            deliveryRouteStop.setKind("pickup".equals(stop.getKind()) ? DELIVERY_ROUTE_STOP_KIND.PICKUP : DELIVERY_ROUTE_STOP_KIND.DROPOFF);
            deliveryRouteStop.setStatus(DELIVERY_ROUTE_STOP_STATUS.PENDING);
            deliveryRouteStop.setStopOrder(stop.getOrder());
            deliveryRouteStop.setLatitude(stop.getLat());
            deliveryRouteStop.setLongitude(stop.getLng());
            deliveryRouteStop.setAddressLabel(stop.getAddressLabel());
            deliveryRouteStop.setPlannedWeightKg(pkg.getWeight() == null ? 0d : pkg.getWeight().doubleValue());
            deliveryRouteStop.setPlannedVolumeCm3((pkg.getHeight() == null ? 0d : pkg.getHeight()) * (pkg.getWidth() == null ? 0d : pkg.getWidth()) * (pkg.getDepth() == null ? 0d : pkg.getDepth()));
            deliveryRouteStops.add(deliveryRouteStop);
        }
        deliveryRoute.getStops().addAll(deliveryRouteStops);
        return deliveryRoutes.saveAndFlush(deliveryRoute);
    }

    private void refreshDeliveryRouteMetrics(DeliveryRoute deliveryRoute, List<DeliveryRouteStop> stops) {
        if (deliveryRoute == null) {
            return;
        }
        List<DeliveryRouteStop> orderedStops = stops == null ? List.of() : stops.stream()
                .sorted(Comparator.comparingInt(stop -> stop.getStopOrder() == null ? Integer.MAX_VALUE : stop.getStopOrder()))
                .collect(Collectors.toList());
        GeoPoint start = new GeoPoint(deliveryRoute.getStartLatitude(), deliveryRoute.getStartLongitude());
        GeoPoint end = new GeoPoint(deliveryRoute.getEndLatitude(), deliveryRoute.getEndLongitude());
        GeoPoint current = start;
        double totalDistanceMeters = 0d;
        double totalWeightKg = 0d;
        double totalVolumeCm3 = 0d;
        Set<Long> packageIds = new LinkedHashSet<>();
        for (DeliveryRouteStop stop : orderedStops) {
            if (stop.getLatitude() == null || stop.getLongitude() == null) {
                continue;
            }
            totalDistanceMeters += haversineMeters(current.lat, current.lng, stop.getLatitude(), stop.getLongitude());
            current = new GeoPoint(stop.getLatitude(), stop.getLongitude());
            if (stop.getaPackage() != null && packageIds.add(stop.getaPackage().getId())) {
                totalWeightKg += stop.getaPackage().getWeight() == null ? 0d : stop.getaPackage().getWeight().doubleValue();
                totalVolumeCm3 += (stop.getaPackage().getHeight() == null ? 0d : stop.getaPackage().getHeight())
                        * (stop.getaPackage().getWidth() == null ? 0d : stop.getaPackage().getWidth())
                        * (stop.getaPackage().getDepth() == null ? 0d : stop.getaPackage().getDepth());
            }
        }
        totalDistanceMeters += haversineMeters(current.lat, current.lng, end.lat, end.lng);
        deliveryRoute.setTotalDistanceMeters(roundTo(2, totalDistanceMeters));
        deliveryRoute.setEstimatedDurationMinutes(roundTo(1, ((totalDistanceMeters / 1000d) / 35d) * 60d));
        deliveryRoute.setTotalWeightKg(roundTo(2, totalWeightKg));
        deliveryRoute.setTotalVolumeCm3(roundTo(2, totalVolumeCm3));
        List<Package> remainingPackages = packages.findAllById(packageIds);
        double totalPayout = remainingPackages.stream()
                .map(this::toSummaryPackageDTO)
                .mapToDouble(this::resolveDisplayedRouteAmount)
                .sum();
        deliveryRoute.setTotalCourierPayout(roundTo(2, totalPayout));
    }

    private void markRouteStopCompleted(Long deliveryPersonID, Long packageID, DELIVERY_ROUTE_STOP_KIND stopKind) {
        deliveryRouteStops.findFirstPendingStop(deliveryPersonID, packageID, stopKind, ACTIVE_DELIVERY_ROUTE_STATUSES, DELIVERY_ROUTE_STOP_STATUS.PENDING)
                .ifPresent(stop -> {
                    stop.setStatus(DELIVERY_ROUTE_STOP_STATUS.DONE);
                    stop.setCompletedAt(Timestamp.valueOf(LocalDateTime.now()));
                    DeliveryRoute deliveryRoute = stop.getDeliveryRoute();
                    if (deliveryRoute.getStatus() == DELIVERY_ROUTE_STATUS.PLANNED) {
                        deliveryRoute.setStatus(DELIVERY_ROUTE_STATUS.ACTIVE);
                        deliveryRoute.setStartedAt(Timestamp.valueOf(LocalDateTime.now()));
                    }
                    deliveryRouteStops.save(stop);
                    if (deliveryRouteStops.countByRouteAndStatus(deliveryRoute.getId(), DELIVERY_ROUTE_STOP_STATUS.PENDING) == 0) {
                        deliveryRoute.setStatus(DELIVERY_ROUTE_STATUS.COMPLETED);
                        deliveryRoute.setCompletedAt(Timestamp.valueOf(LocalDateTime.now()));
                    }
                    deliveryRoutes.save(deliveryRoute);
                });
    }

    private void notifyCourierIfArrivedAtNextStop(Long deliveryPersonId, PositionDTO currentPosition) {
        deliveryRoutes.findFirstByDeliveryPersonAndStatuses(deliveryPersonId, ACTIVE_DELIVERY_ROUTE_STATUSES)
                .ifPresent(deliveryRoute -> {
                    List<DeliveryRouteStop> pendingStops = deliveryRouteStops.findPendingStopsByRouteIdOrderByStopOrder(
                            deliveryRoute.getId(),
                            DELIVERY_ROUTE_STOP_STATUS.PENDING
                    );
                    if (pendingStops.isEmpty()) {
                        return;
                    }
                    DeliveryRouteStop nextStop = pendingStops.get(0);
                    if (nextStop.getArrivalNotificationSentAt() != null
                            || nextStop.getLatitude() == null
                            || nextStop.getLongitude() == null) {
                        return;
                    }

                    double distanceMeters = haversineMeters(
                            currentPosition.getLatitude().doubleValue(),
                            currentPosition.getLongitude().doubleValue(),
                            nextStop.getLatitude(),
                            nextStop.getLongitude()
                    );
                    if (distanceMeters > stopValidationRadiusMeters) {
                        return;
                    }

                    Package nextPackage = nextStop.getaPackage();
                    if (nextPackage == null || nextPackage.getReference() == null || deliveryRoute.getDeliveryPerson() == null) {
                        return;
                    }

                    String packageReference = nextPackage.getReference();
                    String targetUrl = "/package?id=" + packageReference;
                    String payloadJson = "{\"packageReference\":\"" + packageReference
                            + "\",\"stopKind\":\"" + nextStop.getKind().name()
                            + "\",\"deliveryRouteId\":" + deliveryRoute.getId()
                            + ",\"deliveryRouteStopId\":" + nextStop.getId()
                            + ",\"targetUrl\":\"" + targetUrl + "\"}";
                    NOTIFICATION_EVENT_TYPE eventType = nextStop.getKind() == DELIVERY_ROUTE_STOP_KIND.PICKUP
                            ? NOTIFICATION_EVENT_TYPE.PACKAGE_PICKUP_STOP_ARRIVAL
                            : NOTIFICATION_EVENT_TYPE.PACKAGE_DELIVERY_STOP_ARRIVAL;

                    notificationService.createAndDispatch(
                            deliveryRoute.getDeliveryPerson().getId(),
                            eventType,
                            targetUrl,
                            payloadJson,
                            packageReference
                    );
                    if (nextStop.getKind() == DELIVERY_ROUTE_STOP_KIND.PICKUP
                            && nextPackage.getSender() != null
                            && nextPackage.getSender().getId() != null) {
                        notificationService.createAndDispatch(
                                nextPackage.getSender().getId(),
                                NOTIFICATION_EVENT_TYPE.PACKAGE_COURIER_ARRIVED_FOR_PICKUP,
                                targetUrl,
                                payloadJson,
                                packageReference
                        );
                    }
                    nextStop.setArrivalNotificationSentAt(Timestamp.valueOf(LocalDateTime.now()));
                    deliveryRouteStops.save(nextStop);
                });
    }

    private void validateCourierPresenceAtStop(Long deliveryPersonID, Package aPackage, DELIVERY_ROUTE_STOP_KIND stopKind, PositionDTO currentPosition) {
        if (currentPosition == null || currentPosition.getLatitude() == null || currentPosition.getLongitude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "errorDeliveryPersonLocationRequired");
        }

        GeoPoint currentPoint = new GeoPoint(
                currentPosition.getLatitude().doubleValue(),
                currentPosition.getLongitude().doubleValue()
        );

        Optional<DeliveryRouteStop> plannedStop = deliveryRouteStops.findFirstPendingStop(
                deliveryPersonID,
                aPackage.getId(),
                stopKind,
                ACTIVE_DELIVERY_ROUTE_STATUSES,
                DELIVERY_ROUTE_STOP_STATUS.PENDING
        );

        GeoPoint targetPoint = plannedStop
                .map(stop -> new GeoPoint(stop.getLatitude(), stop.getLongitude()))
                .orElseGet(() -> {
                    Address targetAddress = stopKind == DELIVERY_ROUTE_STOP_KIND.PICKUP
                            ? getDepartureAddress(aPackage.getAddresses())
                            : getArrivalAddress(aPackage.getAddresses());
                    if (targetAddress == null || targetAddress.getLatitude() == null || targetAddress.getLongitude() == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "errorRouteStopLocationMissing");
                    }
                    return new GeoPoint(targetAddress.getLatitude().doubleValue(), targetAddress.getLongitude().doubleValue());
                });

        double distanceMeters = haversineMeters(currentPoint.lat, currentPoint.lng, targetPoint.lat, targetPoint.lng);
        if (distanceMeters > stopValidationRadiusMeters) {
            plannedStop.ifPresent(stop -> persistCourierPenalty(
                    stop.getDeliveryRoute() == null ? null : stop.getDeliveryRoute().getDeliveryPerson(),
                    stop.getDeliveryRoute(),
                    aPackage,
                    COURIER_PENALTY_TYPE.STOP_VALIDATION_OUT_OF_RANGE,
                    1,
                    "stopValidationOutOfRange",
                    "{\"deliveryRouteId\":" + (stop.getDeliveryRoute() == null ? null : stop.getDeliveryRoute().getId())
                            + ",\"packageId\":" + aPackage.getId()
                            + ",\"stopKind\":\"" + stopKind.name()
                            + "\",\"distanceMeters\":" + roundTo(2, distanceMeters)
                            + ",\"allowedMeters\":" + roundTo(2, stopValidationRadiusMeters)
                            + "}",
                    true
            ));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "errorDeliveryPersonNotAtStop");
        }
    }

    private void persistCourierPenalty(User deliveryPerson,
                                       DeliveryRoute deliveryRoute,
                                       Package aPackage,
                                       COURIER_PENALTY_TYPE type,
                                       int severity,
                                       String reason,
                                       String detailsJson,
                                       boolean deduplicateRecent) {
        if (deliveryPerson == null || deliveryPerson.getId() == null || type == null) {
            return;
        }
        if (deliveryRoute != null
                && deliveryRoute.getId() != null
                && courierPenalties.existsByDeliveryRouteIdAndTypeAndStatus(deliveryRoute.getId(), type, COURIER_PENALTY_STATUS.ACTIVE)) {
            return;
        }
        if (deduplicateRecent) {
            Timestamp oneHourAgo = Timestamp.valueOf(LocalDateTime.now().minusHours(1));
            long recentCount = courierPenalties.countRecentByDeliveryPersonAndType(
                    deliveryPerson.getId(),
                    type,
                    EnumSet.of(COURIER_PENALTY_STATUS.ACTIVE),
                    oneHourAgo
            );
            if (recentCount > 0) {
                return;
            }
        }
        CourierPenalty penalty = new CourierPenalty();
        penalty.setDeliveryPerson(deliveryPerson);
        penalty.setDeliveryRoute(deliveryRoute);
        penalty.setaPackage(aPackage);
        penalty.setType(type);
        penalty.setStatus(COURIER_PENALTY_STATUS.ACTIVE);
        penalty.setSeverity(severity);
        penalty.setReason(reason);
        penalty.setDetailsJson(detailsJson);
        penalty.setFinancialPenaltyAmount(resolveFinancialPenaltyAmount(type, severity));
        penalty.setCurrency("EUR");
        penalty.setSuspensionUntil(resolveSuspensionUntil(type, severity));
        penalty.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        courierPenalties.save(penalty);
    }

    private Optional<CourierPenalty> findActiveSuspension(Long deliveryPersonId) {
        if (deliveryPersonId == null) {
            return Optional.empty();
        }
        List<CourierPenalty> activeSuspensions = courierPenalties.findActiveSuspensions(
                deliveryPersonId,
                COURIER_PENALTY_STATUS.ACTIVE,
                Timestamp.valueOf(LocalDateTime.now())
        );
        return activeSuspensions.stream().findFirst();
    }

    private void enforceCourierNotSuspended(Long deliveryPersonId) {
        findActiveSuspension(deliveryPersonId)
                .ifPresent(penalty -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "errorCourierSuspended");
                });
    }

    private double applyActiveFinancialPenalties(User deliveryPerson, Double payoutAmount) {
        double payout = safeDouble(payoutAmount);
        if (deliveryPerson == null || deliveryPerson.getId() == null || payout <= 0d) {
            return payout;
        }
        double penaltyAmount = courierPenalties.findByDeliveryPersonIdOrderByCreatedAtDesc(deliveryPerson.getId()).stream()
                .filter(penalty -> COURIER_PENALTY_STATUS.ACTIVE.equals(penalty.getStatus()))
                .filter(penalty -> penalty.getFinancialPenaltyAmount() != null && penalty.getFinancialPenaltyAmount() > 0d)
                .mapToDouble(CourierPenalty::getFinancialPenaltyAmount)
                .sum();
        if (penaltyAmount <= 0d) {
            return payout;
        }
        return roundMoney(Math.max(0d, payout - penaltyAmount));
    }

    private double resolveFinancialPenaltyAmount(COURIER_PENALTY_TYPE type, int severity) {
        if (type == null) {
            return 0d;
        }
        return switch (type) {
            case STOP_VALIDATION_OUT_OF_RANGE -> 0d;
            case ROUTE_PACKAGE_CANCELLED -> 2d;
            case ROUTE_NOT_STARTED_DEADLINE -> 5d;
            case ROUTE_CANCELLED_BEFORE_START -> 7d;
            case ROUTE_STARTED_WITHOUT_PENDING_STOP -> 10d;
            case ROUTE_STARTED_NO_PROGRESS -> 15d;
            case ROUTE_CANCELLED_AFTER_START -> 25d;
        };
    }

    private Timestamp resolveSuspensionUntil(COURIER_PENALTY_TYPE type, int severity) {
        long suspensionHours = resolveSuspensionHours(type, severity);
        if (suspensionHours <= 0) {
            return null;
        }
        return Timestamp.valueOf(LocalDateTime.now().plusHours(suspensionHours));
    }

    private List<CourierPenaltyDTO> buildCourierPenaltyDTOs(List<CourierPenalty> penalties) {
        if (penalties == null || penalties.isEmpty()) {
            return List.of();
        }
        return penalties.stream()
                .map(this::toCourierPenaltyDTO)
                .collect(Collectors.toList());
    }

    private CourierPenaltyDTO toCourierPenaltyDTO(CourierPenalty penalty) {
        CourierPenaltyDTO dto = new CourierPenaltyDTO();
        dto.setId(penalty.getId());
        User deliveryPerson = penalty.getDeliveryPerson();
        if (deliveryPerson != null) {
            dto.setDeliveryPersonId(deliveryPerson.getId());
            String fullName = Stream.of(deliveryPerson.getFirstName(), deliveryPerson.getLastName())
                    .filter(Objects::nonNull)
                    .filter(value -> !value.isBlank())
                    .collect(Collectors.joining(" "));
            dto.setDeliveryPersonName(fullName.isBlank() ? deliveryPerson.getEmailAddress() : fullName);
            dto.setDeliveryPersonEmail(deliveryPerson.getEmailAddress());
        }
        if (penalty.getDeliveryRoute() != null) {
            dto.setDeliveryRouteId(penalty.getDeliveryRoute().getId());
        }
        Package aPackage = penalty.getaPackage();
        if (aPackage != null) {
            dto.setPackageId(aPackage.getId());
            dto.setPackageReference(aPackage.getReference());
        }
        dto.setType(penalty.getType() == null ? null : penalty.getType().name());
        dto.setStatus(penalty.getStatus() == null ? null : penalty.getStatus().name());
        dto.setSeverity(penalty.getSeverity());
        dto.setReason(penalty.getReason());
        dto.setDetailsJson(penalty.getDetailsJson());
        dto.setFinancialPenaltyAmount(penalty.getFinancialPenaltyAmount());
        dto.setCurrency(penalty.getCurrency());
        dto.setSuspensionUntil(penalty.getSuspensionUntil());
        dto.setCreatedAt(penalty.getCreatedAt());
        dto.setReviewedAt(penalty.getReviewedAt());
        return dto;
    }

    private long resolveSuspensionHours(COURIER_PENALTY_TYPE type, int severity) {
        if (type == null) {
            return 0;
        }
        return switch (type) {
            case STOP_VALIDATION_OUT_OF_RANGE, ROUTE_PACKAGE_CANCELLED -> 0;
            case ROUTE_NOT_STARTED_DEADLINE -> 2;
            case ROUTE_CANCELLED_BEFORE_START -> 6;
            case ROUTE_STARTED_WITHOUT_PENDING_STOP -> 12;
            case ROUTE_STARTED_NO_PROGRESS -> 24;
            case ROUTE_CANCELLED_AFTER_START -> 48;
        };
    }

    private RoutePlanDTO toRoutePlanDTO(DeliveryRoute deliveryRoute) {
        if (deliveryRoute == null) {
            return null;
        }
        RoutePlanDTO dto = new RoutePlanDTO();
        dto.setRouteId(deliveryRoute.getId());
        dto.setMode("persistedRoute");
        dto.setStatus(deliveryRoute.getStatus() == null ? null : deliveryRoute.getStatus().name());
        dto.setStart(toRoutePlanPoint(new GeoPoint(deliveryRoute.getStartLatitude(), deliveryRoute.getStartLongitude())));
        dto.setEnd(toRoutePlanPoint(new GeoPoint(deliveryRoute.getEndLatitude(), deliveryRoute.getEndLongitude())));
        List<DeliveryRouteStop> persistedStops = deliveryRouteStops.findByRouteIdOrderByStopOrder(deliveryRoute.getId());
        List<RoutePlanStopDTO> stops = new ArrayList<>();
        Map<Long, RoutePlanPackageAnnotationDTO> annotations = new LinkedHashMap<>();
        LinkedHashSet<Long> packageIds = new LinkedHashSet<>();
        for (int index = 0; index < persistedStops.size(); index++) {
            DeliveryRouteStop persistedStop = persistedStops.get(index);
            RoutePlanStopDTO stop = new RoutePlanStopDTO();
            stop.setKind(persistedStop.getKind() == DELIVERY_ROUTE_STOP_KIND.PICKUP ? "pickup" : "dropoff");
            stop.setPackageId(persistedStop.getaPackage().getId());
            stop.setPackageReference(persistedStop.getaPackage().getReference());
            stop.setAddressLabel(persistedStop.getAddressLabel());
            stop.setLat(persistedStop.getLatitude());
            stop.setLng(persistedStop.getLongitude());
            stop.setOrder(index + 1);
            stop.setStatus(persistedStop.getStatus() == null ? null : persistedStop.getStatus().name());
            stop.setCompletedAt(persistedStop.getCompletedAt() == null ? null : persistedStop.getCompletedAt().toInstant().toString());
            stops.add(stop);
            packageIds.add(stop.getPackageId());
            RoutePlanPackageAnnotationDTO annotation = annotations.computeIfAbsent(stop.getPackageId(), ignored -> {
                RoutePlanPackageAnnotationDTO value = new RoutePlanPackageAnnotationDTO();
                value.setSortOrder(stop.getOrder());
                return value;
            });
            if ("pickup".equals(stop.getKind())) {
                annotation.setPickupOrder(stop.getOrder());
                annotation.setSortOrder(Math.min(annotation.getSortOrder(), stop.getOrder()));
            } else {
                annotation.setDropoffOrder(stop.getOrder());
            }
            annotation.setStopCount((annotation.getPickupOrder() != null ? 1 : 0) + (annotation.getDropoffOrder() != null ? 1 : 0));
        }
        dto.setStops(stops);
        dto.setPackageIds(new ArrayList<>(packageIds));
        dto.setPackageAnnotations(annotations);
        RoutePlanMetricsDTO metrics = new RoutePlanMetricsDTO();
        metrics.setTotalDistanceMeters(deliveryRoute.getTotalDistanceMeters());
        metrics.setEstimatedDurationMinutes(deliveryRoute.getEstimatedDurationMinutes());
        metrics.setTotalDisplayedAmount(deliveryRoute.getTotalCourierPayout());
        metrics.setTotalWeightKg(deliveryRoute.getTotalWeightKg());
        metrics.setTotalVolumeCm3(deliveryRoute.getTotalVolumeCm3());
        dto.setMetrics(metrics);
        List<String> googleMapsNavigationUrls = buildGoogleMapsNavigationUrls(
                new GeoPoint(deliveryRoute.getStartLatitude(), deliveryRoute.getStartLongitude()),
                new GeoPoint(deliveryRoute.getEndLatitude(), deliveryRoute.getEndLongitude()),
                stops
        );
        dto.setGoogleMapsNavigationUrls(googleMapsNavigationUrls);
        dto.setGoogleMapsNavigationUrl(googleMapsNavigationUrls.isEmpty() ? null : googleMapsNavigationUrls.get(0));
        dto.setSelectedPackageId(packageIds.stream().findFirst().orElse(null));
        return dto;
    }

    private double roundTo(int scale, double value) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private static final class GeoPoint {
        private final double lat;
        private final double lng;

        private GeoPoint(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }
    }

    private record PlannablePackage(Long packageId,
                                    String packageReference,
                                    String pickupLabel,
                                    String dropoffLabel,
                                    GeoPoint pickupPoint,
                                    GeoPoint dropoffPoint,
                                    double displayedAmount,
                                    double weightKg,
                                    double volumeCm3,
                                    PackageDTO packageDTO) {
    }

    private record RouteStopCandidate(String kind,
                                      PlannablePackage plannablePackage,
                                      GeoPoint point,
                                      double routeProgress,
                                      int typePriority) {
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

    private Double sumTrackingHttpRequests() {
        try {
            return meterRegistry.find("http.server.requests").meters().stream()
                    .filter(meter -> {
                        String uri = meter.getId().getTag("uri");
                        return uri != null && uri.contains("/packages/v1/tracking/");
                    })
                    .mapToDouble(meter -> readStatistic(meter, Statistic.COUNT))
                    .sum();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Double sumTrackingOperations() {
        try {
            return meterRegistry.find("quickdelivery.packages.operation.calls").meters().stream()
                    .filter(meter -> {
                        String operation = meter.getId().getTag("operation");
                        return operation != null && (
                                operation.equals("trackingSubscriptionLookup")
                                        || operation.equals("updateTrackingPosition")
                                        || operation.equals("updateTrackingPositionByPackageReference")
                                        || operation.equals("trackingContext")
                        );
                    })
                    .mapToDouble(meter -> readStatistic(meter, Statistic.COUNT))
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

    private record CachedRouteDistance(double distanceInKilometers, String formattedDistance, long expiresAtEpochMs) {
    }

    private int resolveCapacityForMode(String mode) {
        if (mode == null) return 3;
        return switch (mode) {
            case "ON_FOOT", "BIKE" -> 5;
            case "SCOOTER" -> 8;
            case "CAR" -> 25;
            case "VAN" -> 50;
            case "TRUCK", "HEAVY" -> 100;
            default -> 10;
        };
    }

    private int resolveCapacityForVehicleType(String vehicleType) {
        if (vehicleType == null || vehicleType.isBlank()) {
            return -1;
        }
        String normalizedVehicleType = vehicleType.trim().toUpperCase(Locale.ROOT);
        return switch (normalizedVehicleType) {
            case "ON_FOOT", "FOOT" -> 3;
            case "BIKE", "BICYCLE" -> 5;
            case "SCOOTER", "MOTORBIKE", "MOTORCYCLE" -> 8;
            case "CAR" -> 12;
            case "VAN", "LARGE_VAN" -> 40;
            case "SMALL_TRUCK" -> 25;
            case "TRUCK" -> 60;
            case "HEAVY", "SEMI_TRAILER" -> 100;
            default -> -1;
        };
    }

    private int resolveMaxPackagesForSearch(String deliveryMode, String vehicleType) {
        int vehicleCapacity = resolveCapacityForVehicleType(vehicleType);
        if (vehicleCapacity > 0) {
            return vehicleCapacity;
        }
        return resolveCapacityForMode(deliveryMode);
    }

    private int resolveUserCapacity(User user) {
        if (user == null) {
            return 1;
        }
        if (user.getVehicles() != null && !user.getVehicles().isEmpty()) {
            Vehicle vehicle = user.getVehicles().iterator().next();
            if (vehicle != null) {
                int vehicleCapacity = resolveCapacityForVehicleType(vehicle.getType());
                if (vehicleCapacity > 0) {
                    return vehicleCapacity;
                }
            }
        }
        return resolveCapacityForMode(user.getDeliveryMode());
    }

    private double resolveSearchRadiusForMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return 10000d;
        }
        return switch (mode) {
            case "ON_FOOT" -> 1000d;
            case "BIKE" -> 3000d;
            case "SCOOTER" -> 8000d;
            case "CAR" -> 15000d;
            case "VAN" -> 30000d;
            case "TRUCK" -> 50000d;
            case "HEAVY" -> 100000d;
            default -> 10000d;
        };
    }

    private double resolveCorridorWidth(String mode, double routeDist) {
        if (mode == null) return Math.max(700d, Math.min(routeDist * 0.12d, 3500d));
        return switch (mode) {
            case "ON_FOOT", "BIKE" -> 800d;
            case "SCOOTER" -> 1500d;
            case "CAR" -> 5000d;
            case "VAN", "TRUCK" -> 12000d;
            case "HEAVY" -> 30000d;
            default -> 2500d;
        };
    }

    private double resolveMetersPerMinute(String mode) {
        if (mode == null || mode.isBlank()) {
            return 400d;
        }
        return switch (mode) {
            case "ON_FOOT" -> 70d;
            case "BIKE" -> 250d;
            case "SCOOTER" -> 450d;
            case "CAR" -> 500d;
            case "VAN" -> 430d;
            case "TRUCK", "HEAVY" -> 360d;
            default -> 400d;
        };
    }

    private double convertDetourMetersToMinutes(double detourMeters, String mode) {
        return detourMeters / Math.max(1d, resolveMetersPerMinute(mode));
    }

    private boolean isOnPathCandidate(double distanceToRoute, double progress, double corridorMeters) {
        return distanceToRoute <= corridorMeters && progress >= -0.05d && progress <= 1.05d;
    }

    private boolean isPackageCompatibleWithMode(Package pkg, String mode) {
        String size = resolvePackageSizeCategory(pkg);
        return switch (mode) {
            case "ON_FOOT", "BIKE" -> "SMALL".equals(size);
            case "SCOOTER" -> "SMALL".equals(size) || "MEDIUM".equals(size);
            case "CAR" -> !"EXTRA_LARGE".equals(size);
            case "VAN", "TRUCK", "HEAVY" -> true;
            default -> true;
        };
    }

    private boolean isPackageCompatibleForSearch(Package pkg, String deliveryMode, String vehicleType) {
        if (vehicleType != null && !vehicleType.isBlank()) {
            return isPackageCompatibleWithVehicleType(pkg, vehicleType);
        }
        if (deliveryMode != null && !deliveryMode.isBlank()) {
            return isPackageCompatibleWithMode(pkg, deliveryMode);
        }
        return true;
    }

    private boolean isPackageCompatibleWithVehicleType(Package pkg, String vehicleType) {
        String size = resolvePackageSizeCategory(pkg);
        String normalizedVehicleType = vehicleType == null ? "" : vehicleType.trim().toUpperCase(Locale.ROOT);
        return switch (normalizedVehicleType) {
            case "ON_FOOT", "FOOT" -> "SMALL".equals(size);
            case "BIKE", "BICYCLE" -> "SMALL".equals(size);
            case "SCOOTER", "MOTORBIKE", "MOTORCYCLE" -> "SMALL".equals(size) || "MEDIUM".equals(size);
            case "CAR" -> !"EXTRA_LARGE".equals(size);
            case "VAN", "LARGE_VAN", "SMALL_TRUCK", "TRUCK", "HEAVY", "SEMI_TRAILER" -> true;
            default -> false;
        };
    }

    private boolean isPackageCompatibleWithVehicleType(Package pkg, User deliveryPerson) {
        String size = resolvePackageSizeCategory(pkg);
        String mode = deliveryPerson.getDeliveryMode();

        // Default mode-based check
        boolean modeCompatible = isPackageCompatibleWithMode(pkg, mode);
        
        // If it's already compatible via mode, no need to check vehicle (mode is the "declared" capacity)
        if (modeCompatible) {
            return true;
        }

        // If mode check failed, check if specific vehicle type allows it (e.g. specialized vehicle for large packages)
        if (deliveryPerson.getVehicles() != null && !deliveryPerson.getVehicles().isEmpty()) {
            Vehicle vehicle = deliveryPerson.getVehicles().iterator().next();
            if (vehicle != null && vehicle.getType() != null) {
                return switch (vehicle.getType()) {
                    case "VAN", "SMALL_TRUCK", "LARGE_VAN", "SEMI_TRAILER" -> true;
                    case "CAR" -> !"EXTRA_LARGE".equals(size);
                    default -> false;
                };
            }
        }

        return false;
    }

    private String calculatePackageSize(Package pkg) {
        if (pkg == null) return "SMALL";
        float weight = pkg.getWeight() != null ? pkg.getWeight() : 0f;
        float h = pkg.getHeight() != null ? pkg.getHeight() : 0f;
        float w = pkg.getWidth() != null ? pkg.getWidth() : 0f;
        float d = pkg.getDepth() != null ? pkg.getDepth() : 0f;
        float maxDim = Math.max(h, Math.max(w, d));

        if (weight <= 2f && maxDim <= 25f) return "SMALL";
        if (weight <= 10f && maxDim <= 45f) return "MEDIUM";
        if (weight <= 25f && maxDim <= 65f) return "LARGE";
        return "EXTRA_LARGE";
    }

    private String resolvePackageSizeCategory(Package pkg) {
        if (pkg == null) {
            return "SMALL";
        }
        String persistedCategory = normalizePackageSizeCategory(pkg.getPackageSizeCategory());
        return persistedCategory != null ? persistedCategory : calculatePackageSize(pkg);
    }

    private String normalizePackageSizeCategory(String packageSizeCategory) {
        if (packageSizeCategory == null || packageSizeCategory.isBlank()) {
            return null;
        }
        String normalizedCategory = packageSizeCategory.trim().toUpperCase(Locale.ROOT);
        return switch (normalizedCategory) {
            case "SMALL", "MEDIUM", "LARGE", "EXTRA_LARGE" -> normalizedCategory;
            default -> null;
        };
    }

    @Override
    @Transactional
    public void applySoftLock(Long packageId, String userId) {
        packages.findById(packageId).ifPresent(pkg -> {
            if (!PACKAGE_STATUS.NEW.equals(pkg.getStatus())) {
                return;
            }
            pkg.setIsSoftLockedBy(userId);
            pkg.setSoftLockExpiresAt(LocalDateTime.now().plusMinutes(5));
            packages.save(pkg);
        });
    }

    private void enforceCourierCapacity(User user) {
        int maxCapacity = resolveUserCapacity(user);
        long activeReservations = packageReservations.countActiveReservationsByDeliveryPerson(
                user.getId(),
                PACKAGE_RESERVATION_STATUS.ONGOING,
                EnumSet.of(PACKAGE_STATUS.RESERVED, PACKAGE_STATUS.PICKEDUP, PACKAGE_STATUS.INDELIVERY)
        );
        if (activeReservations >= maxCapacity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "errorPackageReservationCapacityReached");
        }
    }

    private Package normalizeSoftLockState(Package aPackage) {
        if (aPackage == null) {
            return null;
        }
        if (isSoftLockExpired(aPackage)) {
            clearSoftLock(aPackage);
        }
        return aPackage;
    }

    private boolean isSoftLockActive(Package aPackage) {
        return aPackage != null
                && aPackage.getIsSoftLockedBy() != null
                && aPackage.getSoftLockExpiresAt() != null
                && aPackage.getSoftLockExpiresAt().isAfter(LocalDateTime.now());
    }

    private boolean isSoftLockExpired(Package aPackage) {
        return aPackage != null
                && aPackage.getIsSoftLockedBy() != null
                && aPackage.getSoftLockExpiresAt() != null
                && !aPackage.getSoftLockExpiresAt().isAfter(LocalDateTime.now());
    }

    private void clearSoftLock(Package aPackage) {
        if (aPackage == null) {
            return;
        }
        aPackage.setIsSoftLockedBy(null);
        aPackage.setSoftLockExpiresAt(null);
    }
}
