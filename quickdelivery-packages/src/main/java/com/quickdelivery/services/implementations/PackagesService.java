package com.quickdelivery.services.implementations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.quickdelivery.config.WebSocketHandler;
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
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
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
            FileHelper.saveFilesInParallel(filesMap, packagesDirectory, aPackage.getReference());
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
                    aPackage.getDocument().stream()
                            .filter(document -> document.getType().equals(DOCUMENT_TYPE.PACKAGE_PICTURE))
                            .collect(Collectors.toList()).forEach(document -> {
                                FileDTO fileDTO = new FileDTO();
                                fileDTO.setData(document.getDocContent());
                                fileDTO.setFileName(document.getDocURL());
                                packageDTO.getFiles().add(fileDTO);
                            });
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
    public List<PackageDTO> getPackagesAroundPosition(String latitude, String longitude, double rayonEnMetres) {
        List<Address> addresses = packages.findAddressAroundPosition(latitude,longitude,rayonEnMetres);
        List<PackageDTO> packageDTOS = addresses.stream()
                .map(address -> {
                    Package aPackage = address.getPackaged();
                    PackageDTO packageDTO = modelMapper.map(aPackage, PackageDTO.class);
                    AddressDTO departureAddress = getDepartureAddress(packageDTO.getAddresses());
                    DistanceMatrix distancePackageUser = GeoHelper.getDistanceByCoordinates(geoApiContext, departureAddress.getLatitude().doubleValue(),
                            departureAddress.getLongitude().doubleValue()
                            , Double.parseDouble(latitude), Double.parseDouble(longitude));
                    packageDTO.setFromYou(distancePackageUser.rows[0].elements[0].duration+"/"+distancePackageUser.rows[0].elements[0].distance);
                    return packageDTO;
                })
                .collect(Collectors.toList());
        return packageDTOS;
    }

    @Override
    public List<PackageDTO> findAddressOnMyRoad(String departureLatitude, String arrivalLatitude, String departureLongitude, String arrivalLongitude) {
        if(departureLatitude.compareTo(arrivalLatitude) > 0){
            String permut = departureLatitude;
            departureLatitude = arrivalLatitude;
            arrivalLatitude = permut;
            permut = departureLongitude;
            departureLongitude = arrivalLongitude;
            arrivalLongitude = permut;
        }
        List<Package> packages = this.packages.findPackagesOnMyRoadByRadius(departureLatitude,departureLongitude,5000,arrivalLatitude,arrivalLongitude,5000);
        List<PackageDTO> packageDTOS = new ArrayList<>();
        packages.stream().filter(aPackage -> aPackage.getAddresses().size()==2).collect(Collectors.toList())
                .stream().forEach(aPackage -> packageDTOS.add(modelMapper.map(aPackage, PackageDTO.class)));
        return packageDTOS;
    }

    @Override
    public PackageReservation reservePackage(Long packageID, Long deliveryPersonID, Locale locale) throws NoSuchAlgorithmException {
        Package aPackage = packages.findById(packageID).get();
        User user = users.findById(deliveryPersonID).get();
        aPackage.setStatus(PACKAGE_STATUS.RESERVED);
        PackageReservation packageReservation = new PackageReservation();
        packageReservation.setStatus(PACKAGE_RESERVATION_STATUS.ONGOING);
        packageReservation.setaPackage(aPackage);
        packageReservation.setDeliveryPerson(user);
        packageReservation.setReservationDate(Timestamp.valueOf(LocalDateTime.now()));
        String otp = OTPHelper.generateOTP(OTPSecret, System.currentTimeMillis());
        packageReservation.setPickUpOTP(otp);
        aPackage.getPackageReservations().add(packageReservation);
        packages.save(aPackage);
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
        return modelMapper.map(packages.findById(id), PackageDTO.class);
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
                    aPackage.getDocument().stream()
                            .filter(document -> document.getType().toString().equals(DOCUMENT_TYPE.PACKAGE_PICTURE.toString()))
                            .collect(Collectors.toList()).forEach(document -> {
                                FileDTO fileDTO = new FileDTO();
                                fileDTO.setData(document.getDocContent());
                                fileDTO.setFileName(document.getDocURL());
                                packageDTO.getFiles().add(fileDTO);
                            });
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
        templateModel.put("followupLink", "http://192.168.1.91:8080/followpackage/"+aPackage.getReference());
        templateModel.put("evaluationLink", "http://192.168.1.91:8080/evaluate/"+aPackage.getReference());
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
        return users.findUsersAroundPosition(address.getLatitude().toString(), address.getLongitude().toString(), 2000000);
    }

    @Override
    public PackageDTO findPackageByReference(String reference) {
        return modelMapper.map(packages.findPackageByReference(reference), PackageDTO.class);
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
