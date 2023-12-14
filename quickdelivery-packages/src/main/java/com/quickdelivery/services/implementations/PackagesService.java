package com.quickdelivery.services.implementations;

import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.entities.Address;
import com.quickdelivery.abstarct.entities.Document;
import com.quickdelivery.abstarct.entities.Package;
import com.quickdelivery.abstarct.entities.User;
import com.quickdelivery.abstarct.helpers.GeoHelper;
import com.quickdelivery.abstarct.helpers.PDFGenerator;
import com.quickdelivery.abstarct.helpers.QRCodeGenerator;
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

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class PackagesService implements IPackagesService {
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
    @Override
    public PackageDTO createNewPackage(PackageDTO packageDTO) {
        try {
            createPackage(packageDTO);
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
    public List<PackageDTO> getPAckagesAroundPosition(String latitude, String longitude, double rayonEnMetres) {
        List<Address> addresses = packages.findAddressAroundPosition(latitude,longitude,rayonEnMetres);
        List<PackageDTO> packageDTOS = new ArrayList<>();
        addresses.stream().forEach(address -> packageDTOS.add(modelMapper.map(address.getPackaged(), PackageDTO.class)));
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
    public void reservePackage(Long packageID, Long deliveryPersonID) {
        Package aPackage = packages.findById(packageID).get();
        User user = users.findById(deliveryPersonID).get();
        aPackage.setStatus(PACKAGE_STATUS.RESERVED);
        aPackage.setDeliveryPerson(user);
        packages.save(aPackage);
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

    private void createPackage(PackageDTO packageDTO) throws MalformedURLException, FileNotFoundException {
        packageDTO.getAddresses().stream().forEach(addressDTO -> GeoHelper.AdressGeoCoding(addressDTO, mapQuestURL1, mapQuestKey, mapQuestURL2));
        Package aPackage = modelMapper.map(packageDTO, Package.class);
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
    }
}
