package com.quickdelivery.services.implementations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DistanceMatrix;
import com.quickdelivery.abstarct.dto.AddressDTO;
import com.quickdelivery.abstarct.dto.FileDTO;
import com.quickdelivery.abstarct.dto.MessageDTO;
import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.entities.*;
import com.quickdelivery.abstarct.entities.Package;
import com.quickdelivery.abstarct.helpers.*;
import com.quickdelivery.abstarct.parameters.ADDRESS_TYPE;
import com.quickdelivery.abstarct.parameters.CHECK_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;
import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;
import com.quickdelivery.abstarct.repositories.Packages;
import com.quickdelivery.abstarct.repositories.Users;
import com.quickdelivery.config.WebSocketHandler;
import com.quickdelivery.services.interfaces.IPackagesService;
import jakarta.mail.MessagingException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

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
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private Packages packages;
    @Autowired
    private Users users;
    @Autowired
    private GeoApiContext geoApiContext;
    @Autowired
    WebSocketHandler webSocketHandler;
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
                QRCodeGenerator.generateQRCode(getPackageByIdURL + packageDTO.getId(), packagesDirectory + packageDTO.getReference() + "\\package_qr.png", 150, 150);
            });
            executorService.submit(() -> {
                try {
                    PDFGenerator.generatePdf(packageDTO, packagesDirectory + packageDTO.getReference() + "\\package_qr.png", packagesDirectory + packageDTO.getReference() + "\\package_label.pdf", locale);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            });
            executorService.submit(() -> {
                sendPackageCreationEMail(packageDTO, aPackage, locale);
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
        List<Package> packages = this.packages.findAddressOnMyRoad(departureLatitude,arrivalLatitude,departureLongitude,arrivalLongitude);
        List<PackageDTO> packageDTOS = new ArrayList<>();
        packages.stream().filter(aPackage -> aPackage.getAddresses().size()==2).collect(Collectors.toList())
                .stream().forEach(aPackage -> packageDTOS.add(modelMapper.map(aPackage, PackageDTO.class)));
        return packageDTOS;
    }

    @Override
    public void reservePackage(Long packageID, Long deliveryPersonID) throws NoSuchAlgorithmException {
        Package aPackage = packages.findById(packageID).get();
        User user = users.findById(deliveryPersonID).get();
        aPackage.setStatus(PACKAGE_STATUS.RESERVED);
        PackageReservation packageReservation = new PackageReservation();
        packageReservation.setaPackage(aPackage);
        packageReservation.setDeliveryPerson(user);
        packageReservation.setReservationDate(Timestamp.valueOf(LocalDateTime.now()));
        packageReservation.setPickUpOTP(OTPHelper.generateOTP(OTPSecret, OTPCounter));
        aPackage.getPackageReservations().add(packageReservation);
        //To-Do Send OTP to delivery person & Sender
        packages.save(aPackage);
    }

    @Override
    public void pickUpPackage(Long packageID, Long deliveryPersonID, String pickUpOTP) throws NoSuchAlgorithmException {
        Package aPackage = packages.findById(packageID).get();
        List<PackageReservation> packageReservation_ = aPackage.getPackageReservations().stream().filter(packageReservation -> packageReservation.getDeliveryPerson().getId() == deliveryPersonID).collect(Collectors.toList());
        if(packageReservation_.size()>0 && packageReservation_.get(0).getPickUpOTP().equals(pickUpOTP)) {
            aPackage.setStatus(PACKAGE_STATUS.PICKEDUP);
            aPackage.getPackageReservations().stream().forEach(packageReservation -> {
                try {
                    packageReservation.setPickUpOTP(OTPHelper.generateOTP(OTPSecret, OTPCounter));
                    //To-Do Send OTP to delivery person & receiver
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            });
        }else{
            System.out.println("Unauthorized USER");
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
        aPackage.setSender(users.findById(packageDTO.getSenderID()).get());
        aPackage.setCreationDate(Timestamp.valueOf(LocalDateTime.now()));
        reference.append("PACK").append(packageDTO.getAddresses().get(0).getCountry().substring(0,2).toUpperCase()).append(aPackage.getCreationDate().toString().replaceAll("[\\s\\-:.]", ""));
        aPackage.setReference(reference.toString());
        packageDTO.setReference(reference.toString());
        packageDTO.setCreationDate(aPackage.getCreationDate());
        //GeoHelper.getDirection(geoApiContext, getDepartureAddress(packageDTO.getAddresses()).toString(), getArrivalAddress(packageDTO.getAddresses()).toString());
        packageDTO.setId(aPackage.getId());
        packageDTO.setVersion(aPackage.getVersion());
        Document qrDocument = new Document();
        qrDocument.setaPackage(aPackage);
        qrDocument.setType(DOCUMENT_TYPE.PACKAGE_QR);
        qrDocument.setDocURL(packagesDirectory+packageDTO.getReference()+"\\package_qr.png");
        aPackage.getDocument().add(qrDocument);
        Document pdfDocument = new Document();
        pdfDocument.setaPackage(aPackage);
        pdfDocument.setType(DOCUMENT_TYPE.PACKAGE_PDF_LABEL);
        pdfDocument.setDocURL(packagesDirectory+packageDTO.getReference()+"\\package_label.pdf");
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

    private void sendPackageCreationEMail(PackageDTO packageDTO, Package aPackage, Locale locale){
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("recipientName", getDepartureAddress(packageDTO.getAddresses()).getFirstName()+" "+getDepartureAddress(packageDTO.getAddresses()).getLastName());
        templateModel.put("height", aPackage.getHeight());
        templateModel.put("width", aPackage.getWidth());
        templateModel.put("depth", aPackage.getDepth());
        templateModel.put("weight", aPackage.getWeight());
        templateModel.put("deliveryPrice", aPackage.getDeliveryPrice());
        templateModel.put("departureAddress", getDepartureAddress(packageDTO.getAddresses()).formatedtoString());
        templateModel.put("pickupDateTime", getDepartureAddress(packageDTO.getAddresses()).getDateTime());
        templateModel.put("arrivalAddress", getArrivalAddress(packageDTO.getAddresses()).formatedtoString());
        templateModel.put("deliveryDateTime", getArrivalAddress(packageDTO.getAddresses()).getDateTime());
        try {
            MailHelper.sendMessageUsingThymeleafTemplate(getDepartureAddress(packageDTO.getAddresses()).getEmail(),"New Package Created",templateModel, locale, "newpackage-template-thymeleaf.html",packagesDirectory+packageDTO.getReference()+"\\package_label.pdf");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
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

    public List<Address> findUsersAroundPosition(String aPackage){
        Address address = getDepartureAddress(packages.findPackageByReference(aPackage).getAddresses());
        return users.findUsersAroundPosition(address.getLatitude().toString(), address.getLongitude().toString(), 2000000);
    }

    @Override
    public PackageDTO findPackageByReference(String reference) {
        return modelMapper.map(packages.findPackageByReference(reference), PackageDTO.class);
    }
}
