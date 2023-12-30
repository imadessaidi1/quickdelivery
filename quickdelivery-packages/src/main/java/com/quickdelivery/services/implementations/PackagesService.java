package com.quickdelivery.services.implementations;

import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DistanceMatrix;
import com.quickdelivery.abstarct.dto.AddressDTO;
import com.quickdelivery.abstarct.dto.FileDTO;
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
import com.quickdelivery.services.interfaces.IPackagesService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
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
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private Packages packages;
    @Autowired
    private Users users;
    @Autowired
    private GeoApiContext geoApiContext;
    @Override
    public PackageDTO createNewPackage(PackageDTO packageDTO, MultipartFile[] files) {
        try {
            Package aPackage = createPackage(packageDTO);
            IntStream.range(0, files.length)
                    .forEach(index -> {
                        MultipartFile file = files[index];
                        Document document = new Document();
                        document.setaPackage(aPackage);
                        document.setDocURL(file.getName());
                        document.setType(index == 0 ? DOCUMENT_TYPE.PACKAGE_PICTURE : DOCUMENT_TYPE.PACKAGE_INVOICE);
                        try {
                            document.setDocContent(file.getBytes());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        aPackage.getDocument().add(document);
                    });
            packages.save(aPackage);
        } catch (MalformedURLException | FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return packageDTO;
    }

    @Override
    public void createNewPackages(List<PackageDTO> packageDTOS) {
        packageDTOS.stream().forEach(packageDTO -> {
            try {
                createPackage(packageDTO);
            } catch (MalformedURLException | FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Map<String, List<PackageDTO>> getPAckagesAroundPosition(String latitude, String longitude, double rayonEnMetres) {
        List<Address> addresses = packages.findAddressAroundPosition(latitude,longitude,rayonEnMetres);
        List<PackageDTO> packageDTOS = new ArrayList<>();
        addresses.stream().forEach(address -> {
            Package aPackage = address.getPackaged();
            PackageDTO packageDTO = modelMapper.map(aPackage, PackageDTO.class);
            aPackage.getDocument().stream().forEach(document -> {
                if(document.getType().equals(DOCUMENT_TYPE.PACKAGE_PICTURE)) {
                    FileDTO fileDTO = new FileDTO();
                    fileDTO.setData(document.getDocContent());
                    fileDTO.setFileName(document.getDocURL());
                    packageDTO.getFiles().add(fileDTO);
                }
            });
            packageDTOS.add(packageDTO);
        });
        Map<String, List<PackageDTO>> groupedPackages = packageDTOS.stream()
                .collect(Collectors.groupingBy(packaged -> {
                    AddressDTO departureAddress = getDepartureAddress(packaged.getAddresses());
                    AddressDTO arrivalAddress = getArrivalAddress(packaged.getAddresses());
                    DistanceMatrix distancePackageUser = GeoHelper.getDistanceByCoordinates(geoApiContext, departureAddress.getLatitude().doubleValue(),
                            departureAddress.getLongitude().doubleValue()
                            , Double.parseDouble(latitude), Double.parseDouble(longitude));
                    return departureAddress.getLatitude()+","+departureAddress.getLongitude()+","+departureAddress.toString()
                            +" ("+distancePackageUser.rows[0].elements[0].duration+"/"+distancePackageUser.rows[0].elements[0].distance+")";
                }));
        return groupedPackages;
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

    private Package createPackage(PackageDTO packageDTO) throws MalformedURLException, FileNotFoundException {
        packageDTO.getAddresses().stream().forEach(addressDTO -> {
            try {
                GeoHelper.AdressGeoCoding(geoApiContext, addressDTO);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ApiException e) {
                throw new RuntimeException(e);
            }
        });
        Package aPackage = modelMapper.map(packageDTO, Package.class);
        aPackage.setDeliveryPrice(PackageDeliveryPriceCalculator.calculateDeliveryPrice(geoApiContext, packageDTO));
        packageDTO.setDeliveryPrice(aPackage.getDeliveryPrice());
        aPackage.getAddresses().stream().forEach(address -> address.setPackaged(aPackage));
        aPackage.setSender(users.findById(packageDTO.getSenderID()).get());
        aPackage.setCreationDate(Timestamp.valueOf(LocalDateTime.now()));
        packages.save(aPackage);
        packageDTO.setId(aPackage.getId());
        packageDTO.setVersion(aPackage.getVersion());
        QRCodeGenerator.generateQRCode(getPackageByIdURL+packageDTO.getId(),qrCodePath+packageDTO.getId()+".png",150,150);
        Document qrDocument = new Document();
        qrDocument.setaPackage(aPackage);
        qrDocument.setType(DOCUMENT_TYPE.PACKAGE_QR);
        qrDocument.setDocURL(qrCodePath+packageDTO.getId()+".png");
        Set<Document> documents = new HashSet<>();
        documents.add(qrDocument);
        PDFGenerator.generatePdf(packageDTO, qrCodePath+packageDTO.getId()+".png", qrCodePath+packageDTO.getId()+".pdf");
        Document pdfDocument = new Document();
        pdfDocument.setaPackage(aPackage);
        pdfDocument.setType(DOCUMENT_TYPE.PACKAGE_PDF_LABEL);
        pdfDocument.setDocURL(qrCodePath+packageDTO.getId()+".pdf");
        documents.add(pdfDocument);
        aPackage.setDocument(documents);
        packages.save(aPackage);
        return aPackage;
    }

    private AddressDTO getDepartureAddress(List<AddressDTO> addresses) {
        for (AddressDTO address : addresses) {
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
}
