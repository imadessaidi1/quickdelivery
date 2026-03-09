package com.quickdelivery.services.implementations;

import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DistanceMatrix;
import com.quickdelivery.abstarct.dto.*;
import com.quickdelivery.abstarct.entities.*;
import com.quickdelivery.abstarct.entities.Package;
import com.quickdelivery.abstarct.helpers.*;
import com.quickdelivery.abstarct.parameters.*;
import com.quickdelivery.abstarct.repositories.Packages;
import com.quickdelivery.abstarct.repositories.Users;
import com.quickdelivery.dto.ReserveBatchResultDTO;
import com.quickdelivery.services.interfaces.IPackagesService;
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
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional
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
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private Packages packages;
    @Autowired
    private Users users;
    @Autowired
    private GeoApiContext geoApiContext;
    @Autowired
    private Logger logger;
    @Autowired
    private ResourceBundleMessageSource messageSource;
    @Autowired()
    @Qualifier("myTemplateResolver")
    private ITemplateResolver templateResolver;
    @Override
    public PackageDTO createNewPackage(PackageDTO packageDTO, MultipartFile[] files, Locale locale) {
        MultiValueMap<String, MultipartFile> filesMap = new LinkedMultiValueMap<>();
        ExecutorService executorService;
        Package aPackage;
        try {
            executorService = Executors.newFixedThreadPool(3);
            packageAddressGeocoding(packageDTO);
            packageDistanceCalculation(packageDTO);
            aPackage = preparPackage(packageDTO, locale);
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
            packages.save(aPackage);
            packageDTO.setId(aPackage.getId());
            packageDTO.setVersion(aPackage.getVersion());
            FileHelper.saveFilesInParallel(filesMap, packagesDirectory+aPackage.getReference(), false);
            executorService.submit(() -> {
                QRCodeGenerator.generateQRCode(getPackageByIdURL + packageDTO.getReference(),
                        packagesDirectory + packageDTO.getReference() + packageQrEnds, 150, 150);
            });
            executorService.submit(() -> {
                try {
                    PDFGenerator.generatePdf(packageDTO, packagesDirectory + packageDTO.getReference() + packageQrEnds,
                            packagesDirectory + packageDTO.getReference() + packageLabelEnds, locale);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            });
            executorService.submit(() -> {
                sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_CREATION.getType(),messageSource.getMessage("email.subject.newPackage", null, locale),EMAIL_TYPE.PACKAGE_CREATION);
            });
        } catch (MalformedURLException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return packageDTO;
    }

    @Override
    public void createNewPackages(List<PackageDTO> packageDTOS, Locale locale) {
        packageDTOS.stream().forEach(packageDTO -> {
            try {
                preparPackage(packageDTO, locale);
            } catch (MalformedURLException | FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Map<String, List<PackageDTO>> getPAckagesAroundPosition(String latitude, String longitude, double rayonEnMetres) {
        List<Address> addresses = packages.findAddressAroundPosition(latitude,longitude,rayonEnMetres);
        List<PackageDTO> packageDTOS = addresses.stream()
                .map(address -> {
                    Package aPackage = address.getPackaged();
                    PackageDTO packageDTO = modelMapper.map(aPackage, PackageDTO.class);
                    aPackage.getDocument().stream().forEach(document -> {
                        if(document.getType().equals(DOCUMENT_TYPE.PACKAGE_PICTURE) ||
                                document.getType().equals(DOCUMENT_TYPE.PACKAGE_INVOICE)){
                            DocumentDTO documentDTO = modelMapper.map(document, DocumentDTO.class);
                            documentDTO.setFileName(document.getType().toString());
                            try {
                                documentDTO.setData(Files.readAllBytes(Paths.get(document.getDocURL())));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            packageDTO.getDocumentS().put(documentDTO.getType(),documentDTO);
                        }
                    });
                    packageDTO.getAddresses().stream().forEach(addressDTO -> addressDTO.setAddressAuto(addressDTO.toString()));
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
                            +" ("+distancePackageUser.rows[0].elements[0].duration+"/"+distancePackageUser.rows[0].elements[0].distance+")";
                }));
        return groupedPackages;
    }

    @Override
    public Map<String, List<PackageDTO>> getPAckagesAroundPosition(AddressDTO address, double rayonEnMetres) throws IOException, InterruptedException, ApiException {
        GeoHelper.AddressGeoCoding(geoApiContext,address);
        return getPAckagesAroundPosition(address.getLatitude().toString(), address.getLongitude().toString(), rayonEnMetres);
    }

    @Override
    public List<PackageDTO> getPackagesAroundPosition(String latitude, String longitude, double rayonEnMetres) {
        List<Address> addresses = packages.findAddressAroundPosition(latitude,longitude,rayonEnMetres);
        List<PackageDTO> packageDTOS = addresses.stream()
                .map(address -> {
                    Package aPackage = address.getPackaged();
                    PackageDTO packageDTO = modelMapper.map(aPackage, PackageDTO.class);
                    aPackage.getDocument().stream().forEach(document -> {
                        if(document.getType().equals(DOCUMENT_TYPE.PACKAGE_PICTURE) ||
                                document.getType().equals(DOCUMENT_TYPE.PACKAGE_INVOICE)){
                            DocumentDTO documentDTO = modelMapper.map(document, DocumentDTO.class);
                            documentDTO.setFileName(document.getType().toString());
                            try {
                                documentDTO.setData(Files.readAllBytes(Paths.get(document.getDocURL())));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            packageDTO.getDocumentS().put(documentDTO.getType(),documentDTO);
                        }
                    });
                    AddressDTO departureAddress = getDepartureAddress(packageDTO.getAddresses());
                    DistanceMatrix distancePackageUser = GeoHelper.getDistanceByCoordinates(geoApiContext, departureAddress.getLatitude().doubleValue(),
                            departureAddress.getLongitude().doubleValue()
                            , Double.parseDouble(latitude), Double.parseDouble(longitude));
                    packageDTO.setFromYou(distancePackageUser.rows[0].elements[0].duration+"/"+distancePackageUser.rows[0].elements[0].distance);
                    packageDTO.getAddresses().stream().forEach(addressDTO -> addressDTO.setAddressAuto(addressDTO.toString()));
                    return packageDTO;
                })
                .collect(Collectors.toList());
        return packageDTOS;
    }

    @Override
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

                    PackageDTO dto = modelMapper.map(pkg, PackageDTO.class);
                    dto.getAddresses().forEach(addressDTO -> addressDTO.setAddressAuto(addressDTO.toString()));
                    return dto;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(PackageDTO::getId))
                .collect(Collectors.toList());
    }

    @Override
    public PackageReservation reservePackage(Long packageID, Long deliveryPersonID, Locale locale) throws NoSuchAlgorithmException {
        PackageReservation packageReservation = reservePackageInternal(packageID, deliveryPersonID);
        Package aPackage = packageReservation.getaPackage();
        /*ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(() -> {*/
            sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_RESERVATION_SENDER.getType(), messageSource.getMessage("email.subject.packageReserved", null, locale),EMAIL_TYPE.PACKAGE_RESERVATION_SENDER);
        /*});
        executorService.submit(() -> {*/
            sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_RESERVATION_DELIVERY.getType(), messageSource.getMessage("email.subject.packageReservationCofirm", null, locale),EMAIL_TYPE.PACKAGE_RESERVATION_DELIVERY);
        //});
        return packageReservation;
    }

    @Override
    public ReserveBatchResultDTO reservePackagesBatch(List<Long> packageIds, Long deliveryPersonID, Locale locale) {
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
                sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_RESERVATION_SENDER.getType(),
                        messageSource.getMessage("email.subject.packageReserved", null, locale), EMAIL_TYPE.PACKAGE_RESERVATION_SENDER);
                sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_RESERVATION_DELIVERY.getType(),
                        messageSource.getMessage("email.subject.packageReservationCofirm", null, locale), EMAIL_TYPE.PACKAGE_RESERVATION_DELIVERY);
                result.getReservedPackageIds().add(packageId);
            } catch (ResponseStatusException ex) {
                result.getSkippedPackages().put(packageId, ex.getReason() == null ? ex.getStatusCode().toString() : ex.getReason());
            } catch (Exception ex) {
                result.getSkippedPackages().put(packageId, "Unexpected error while reserving package");
            }
        }

        result.setReservedCount(result.getReservedPackageIds().size());
        return result;
    }

    @Override
    public void pickUpPackage(Long packageID, Long deliveryPersonID, String pickUpOTP, Locale locale) throws NoSuchAlgorithmException {
        Package aPackage = packages.findById(packageID).get();
        List<PackageReservation> packageReservation_ = aPackage.getPackageReservations().stream().filter(packageReservation -> packageReservation.getDeliveryPerson().getId().equals(deliveryPersonID)).collect(Collectors.toList());
        if(packageReservation_.size()>0 && packageReservation_.get(0).getPickUpOTP().equals(pickUpOTP)) {
            aPackage.setStatus(PACKAGE_STATUS.PICKEDUP);
            try {
                packageReservation_.get(0).setDeliveryOTP(OTPHelper.generateOTP(OTPSecret, System.currentTimeMillis()));
                //To-Do Send OTP to delivery person & receiver
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            packages.save(aPackage);
            /*ExecutorService executorService = Executors.newFixedThreadPool(3);
            executorService.submit(() -> {*/
                sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_PICKUP_SENDER.getType(),
                        messageSource.getMessage("email.subject.packagePickup", null, locale),EMAIL_TYPE.PACKAGE_PICKUP_SENDER);
            /*});
            executorService.submit(() -> {*/
                sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_PICKUP_RECEIVER.getType(),
                        messageSource.getMessage("email.subject.packagePickupR", null, locale),EMAIL_TYPE.PACKAGE_PICKUP_RECEIVER);
            /*});
            executorService.submit(() -> {*/
                sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_PICKUP_DELIVERY.getType(),
                        messageSource.getMessage("email.subject.packagePickupCofirm", null, locale),EMAIL_TYPE.PACKAGE_PICKUP_DELIVERY);
            //});
        }else{
            logger.info("Unauthorized USER");
        }
    }

    public void deliverPackage(Long packageID, Long deliveryPersonID, String pickUpOTP, Locale locale) throws NoSuchAlgorithmException {
        Package aPackage = packages.findById(packageID).get();
        List<PackageReservation> packageReservation_ = aPackage.getPackageReservations().stream().filter(packageReservation -> packageReservation.getDeliveryPerson().getId().equals(deliveryPersonID)).collect(Collectors.toList());
        if(packageReservation_.size()>0 && packageReservation_.get(0).getDeliveryOTP().equals(pickUpOTP)) {
            aPackage.setStatus(PACKAGE_STATUS.DELIVERED);
            packageReservation_.get(0).setStatus(PACKAGE_RESERVATION_STATUS.FINISHED);
            packages.save(aPackage);
            /*ExecutorService executorService = Executors.newFixedThreadPool(3);
            executorService.submit(() -> {*/
            sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_DELIVERY_RECEIVER.getType(),
                    messageSource.getMessage("email.subject.packageDelivered", null, locale),EMAIL_TYPE.PACKAGE_DELIVERY_RECEIVER);
            /*});
            executorService.submit(() -> {*/
            sendPackageCreationEMail(aPackage, locale, EMAIL_TEMPLATE_TYPE.PACKAGE_DELIVERY_SENDER.getType(),
                    messageSource.getMessage("email.subject.packageDelivered", null, locale),EMAIL_TYPE.PACKAGE_DELIVERY_SENDER);
            //});
        }else{
            logger.info("Unauthorized USER");
        }
    }

    @Override
    public CHECK_STATUS checkOTPForPickUpPackage(Long packageID, Long senderID, String pickUpOTP) {
        Package aPackage = packages.findById(packageID).get();
        List<PackageReservation> packageReservation_ = aPackage.getPackageReservations().stream().filter(packageReservation -> packageReservation.getPickUpOTP().equals(pickUpOTP)).collect(Collectors.toList());
        if(packageReservation_.size()>0 && aPackage.getSender().getId().equals(senderID)) {
           return CHECK_STATUS.OK;
        }else{
            return CHECK_STATUS.KO;
        }
    }

    @Override
    public CHECK_STATUS checkOTPForDeliverPackage(Long packageID, Long deliveryPersonID, String deliveryOTP) {
        Package aPackage = packages.findById(packageID).get();
        List<PackageReservation> packageReservation_ = aPackage.getPackageReservations().stream().filter(packageReservation -> packageReservation.getDeliveryOTP().equals(deliveryOTP)).collect(Collectors.toList());
        if(packageReservation_.size()>0 && aPackage.getSender().getId().equals(deliveryPersonID)) {
            return CHECK_STATUS.OK;
        }else{
            return CHECK_STATUS.KO;
        }
    }

    @Override
    public PackageDTO findPackageByID(Long id) {
        Package aPackage = packages.findById(id).get();
        PackageDTO packageDTO = modelMapper.map(aPackage, PackageDTO.class);
        packageDTO.getDocumentS().clear();
        aPackage.getDocument().stream().forEach(document -> {
            if(document.getType().equals(DOCUMENT_TYPE.PACKAGE_PICTURE) ||
                    document.getType().equals(DOCUMENT_TYPE.PACKAGE_INVOICE)){
                DocumentDTO documentDTO = modelMapper.map(document, DocumentDTO.class);
                documentDTO.setFileName(document.getType().toString());
                try {
                    documentDTO.setData(Files.readAllBytes(Paths.get(document.getDocURL())));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                packageDTO.getDocumentS().put(documentDTO.getType(),documentDTO);
            }
        });
        packageDTO.getAddresses().stream().forEach(addressDTO -> addressDTO.setAddressAuto(addressDTO.toString()));
        return packageDTO;
    }

    @Override
    public PackageDTO updatePackage(PackageDTO user) {
        return null;
    }

    @Override
    public void deletePackage(PackageDTO user) {

    }

    @Override
    public List<PackageDTO> findPackagesByStatus(PACKAGE_STATUS status) {
        List<PackageDTO> packageDTOS = new ArrayList<>();
        List<Package> packages = this.packages.findPackagesByStatus(status);
        packages.stream().forEach(aPackage -> packageDTOS.add(modelMapper.map(aPackage, PackageDTO.class)));
        return packageDTOS;
    }

    @Override
    public void updatePackageStatus(PACKAGE_STATUS status, Long id) {
        packages.updatePackagesStatus(status,id);
    }

    @Override
    public Map<PACKAGE_STATUS, List<PackageDTO>> getPackagesByDeliveryPerson(Long deliveryPersonID) {
        List<Package> packageList = packages.findPackagesByDeliveryPerson(deliveryPersonID);
        List<PackageDTO> packageDTOS = packageList.stream()
                .map(aPackage  -> {
                    PackageDTO packageDTO = modelMapper.map(aPackage, PackageDTO.class);
                    aPackage.getDocument().stream().forEach(document -> {
                        if(document.getType().equals(DOCUMENT_TYPE.PACKAGE_PICTURE) ||
                                document.getType().equals(DOCUMENT_TYPE.PACKAGE_INVOICE)){
                            DocumentDTO documentDTO = modelMapper.map(document, DocumentDTO.class);
                            documentDTO.setFileName(document.getType().toString());
                            try {
                                documentDTO.setData(Files.readAllBytes(Paths.get(document.getDocURL())));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            packageDTO.getDocumentS().put(documentDTO.getType(),documentDTO);
                        }
                    });
                    packageDTO.getAddresses().stream().forEach(addressDTO -> addressDTO.setAddressAuto(addressDTO.toString()));
                    return packageDTO;
                })
                .collect(Collectors.toList());
        Map<PACKAGE_STATUS, List<PackageDTO>> groupedPackages = packageDTOS.parallelStream()
                .collect(Collectors.groupingByConcurrent(packaged -> {
                    return packaged.getStatus();
                }));
        return groupedPackages;
    }

    private Package preparPackage(PackageDTO packageDTO, Locale locale) throws MalformedURLException, FileNotFoundException {
        Package aPackage = modelMapper.map(packageDTO, Package.class);
        StringBuffer reference = new StringBuffer();
        aPackage.setDeliveryPrice(PackageDeliveryPriceCalculator.calculateDeliveryPrice(geoApiContext, packageDTO));
        packageDTO.setDeliveryPrice(aPackage.getDeliveryPrice());
        aPackage.getAddresses().stream().forEach(address -> address.setPackaged(aPackage));
        aPackage.setSender(null);
        aPackage.setCreationDate(Timestamp.valueOf(LocalDateTime.now()));
        reference.append(packageReferenceStart).append(packageDTO.getAddresses().get(0).getCountry().substring(0,2).toUpperCase()).append(aPackage.getCreationDate().toString().replaceAll("[\\s\\-:.]", ""));
        aPackage.setReference(reference.toString());
        packageDTO.setReference(reference.toString());
        packageDTO.setCreationDate(aPackage.getCreationDate());
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

    private void packageAddressGeocoding(PackageDTO aPackage){
        aPackage.getAddresses().stream().forEach(addressDTO -> {
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

    private void packageDistanceCalculation(PackageDTO aPackage){
        DistanceMatrix distancePackageDestination = GeoHelper.getDistanceByAddress(geoApiContext, getDepartureAddress(aPackage.getAddresses()).toString(),
                getArrivalAddress(aPackage.getAddresses()).toString());
        aPackage.setDistanceToDestination(distancePackageDestination.rows[0].elements[0].duration+"/"+distancePackageDestination.rows[0].elements[0].distance);
    }

    private void sendPackageCreationEMail(Package aPackage, Locale locale, String template, String subject, EMAIL_TYPE type){
        Map<String, Object> templateModel = new HashMap<>();
        if(type.equals(EMAIL_TYPE.PACKAGE_CREATION) || type.equals(EMAIL_TYPE.PACKAGE_RESERVATION_SENDER) || type.equals(EMAIL_TYPE.PACKAGE_PICKUP_SENDER)
                ||type.equals(EMAIL_TYPE.PACKAGE_DELIVERY_SENDER)) {
            templateModel.put("recipientName", getDepartureAddress(aPackage.getAddresses()).getFirstName() + " " + getDepartureAddress(aPackage.getAddresses()).getLastName());
        }else if(type.equals(EMAIL_TYPE.PACKAGE_PICKUP_RECEIVER) ||type.equals(EMAIL_TYPE.PACKAGE_DELIVERY_RECEIVER)) {
            templateModel.put("recipientName", getArrivalAddress(aPackage.getAddresses()).getFirstName() + " " + getArrivalAddress(aPackage.getAddresses()).getLastName());
        }
        templateModel.put("height", aPackage.getHeight());
        templateModel.put("width", aPackage.getWidth());
        templateModel.put("depth", aPackage.getDepth());
        templateModel.put("weight", aPackage.getWeight());
        templateModel.put("packageReference", aPackage.getReference());
        templateModel.put("deliveryPrice", aPackage.getDeliveryPrice());
        templateModel.put("followupLink", packageFollowupLink+aPackage.getReference());
        templateModel.put("evaluationLink", evaluateLink+aPackage.getReference());
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
        templateModel.put("departureAddress", getDepartureAddress(aPackage.getAddresses()).formatedtoString());
        templateModel.put("pickupDateTime", getDepartureAddress(aPackage.getAddresses()).getDateTime());
        templateModel.put("arrivalAddress", getArrivalAddress(aPackage.getAddresses()).formatedtoString());
        templateModel.put("deliveryDateTime", getArrivalAddress(aPackage.getAddresses()).getDateTime());
        String attachement;
        if(type.equals(EMAIL_TYPE.PACKAGE_RESERVATION_DELIVERY) || type.equals(EMAIL_TYPE.PACKAGE_PICKUP_DELIVERY) || type.equals(EMAIL_TYPE.PACKAGE_PICKUP_RECEIVER)
                || type.equals(EMAIL_TYPE.PACKAGE_PICKUP_SENDER) || type.equals(EMAIL_TYPE.PACKAGE_DELIVERY_RECEIVER) || type.equals(EMAIL_TYPE.PACKAGE_DELIVERY_SENDER)){
            attachement = null;
        }else{
            attachement = packagesDirectory+aPackage.getReference()+packageLabelEnds;
        }
        try {
            MailHelper.sendMessageUsingThymeleafTemplate(messageSource,templateResolver,getDepartureAddress(aPackage.getAddresses()).getEmail(),
                    subject,templateModel, locale, template,attachement);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Address> findUsersAroundPosition(String aPackage){
        Address address = getDepartureAddress(packages.findPackageByReference(aPackage).getAddresses());
        return users.findUsersAroundPosition(address.getLatitude().toString(), address.getLongitude().toString(), 20000);
    }

    @Override
    public PackageDTO findPackageByReference(String reference) {
        if (reference == null || reference.isBlank() || "undefined".equalsIgnoreCase(reference)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Package reference is required");
        }
        Package aPackage = packages.findPackageByReference(reference);
        if (aPackage == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Package not found for reference: " + reference);
        }
        PackageDTO packageDTO = modelMapper.map(aPackage, PackageDTO.class);
        packageDTO.getDocumentS().clear();
        aPackage.getDocument().stream().forEach(document -> {
            if(document.getType().equals(DOCUMENT_TYPE.PACKAGE_PICTURE) ||
                    document.getType().equals(DOCUMENT_TYPE.PACKAGE_INVOICE)){
                DocumentDTO documentDTO = modelMapper.map(document, DocumentDTO.class);
                documentDTO.setFileName(document.getType().toString());
                try {
                    documentDTO.setData(Files.readAllBytes(Paths.get(document.getDocURL())));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                packageDTO.getDocumentS().put(documentDTO.getType(),documentDTO);
            }
        });
        packageDTO.getAddresses().stream().forEach(addressDTO -> addressDTO.setAddressAuto(addressDTO.toString()));
        return packageDTO;
    }

    @Override
    public boolean isUserWithOngoingDelivery(Long userId) {
        return packages.existsOngoingReservationsForUserWithPickedUpPackage(userId);
    }

    @Override
    public Map<String, PositionDTO> handleWebsocketMessage(MessageDTO messageDTO) {
        Map<String, PositionDTO> map = new HashMap<>();
        if(messageDTO.getType().equals("PACKAGE_POSITION_UPDATE")){
            List<Package> packageList = packages.findPackagesInDeliveryByDeliveryPerson(Long.parseLong(messageDTO.getFrom()));
            packageList.stream().forEach(aPackage -> {
                aPackage.setLastPositionLatitude(messageDTO.getPositionDTO().getLatitude());
                aPackage.setLastPositionLongitude(messageDTO.getPositionDTO().getLongitude());
                packages.save(aPackage);
                map.put(aPackage.getReference(), messageDTO.getPositionDTO());
            });
        }
        return map;
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
        packages.save(aPackage);
        return packageReservation;
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
}
