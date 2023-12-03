package com.quickdelivery.abstarct.helpers;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.quickdelivery.abstarct.dto.AddressDTO;
import com.quickdelivery.abstarct.dto.PackageDTO;
import com.quickdelivery.abstarct.parameters.ADDRESS_TYPE;
import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PackegeCSVReader {
    public static List<PackageDTO> CSVToPackages(MultipartFile file){
        List<PackageDTO> packageDTOFromCsvList = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> records = reader.readAll();
            records.stream().forEach(strings -> {
                PackageDTO packageDTO = new PackageDTO();
                packageDTO.setHeight(Float.valueOf(strings[0]));
                packageDTO.setWidth(Float.valueOf(strings[1]));
                packageDTO.setWeight(Float.valueOf(strings[2]));
                packageDTO.setHeight(Float.valueOf(strings[3]));
                packageDTO.setStatus(PACKAGE_STATUS.NEW);
                List<AddressDTO> packageAddressDTO=new ArrayList<>();
                AddressDTO departureAddressDTO = new AddressDTO();
                departureAddressDTO.setType(ADDRESS_TYPE.DEPARTURE);
                departureAddressDTO.setLine1(strings[4]);
                departureAddressDTO.setLine2(strings[5]);
                departureAddressDTO.setZipCode(strings[6]);
                departureAddressDTO.setTown(strings[7]);
                departureAddressDTO.setCountry(strings[8]);
                packageAddressDTO.add(departureAddressDTO);
                AddressDTO arrivalAddressDTO = new AddressDTO();
                arrivalAddressDTO.setType(ADDRESS_TYPE.ARRIVAL);
                arrivalAddressDTO.setLine1(strings[9]);
                arrivalAddressDTO.setLine2(strings[10]);
                arrivalAddressDTO.setZipCode(strings[11]);
                arrivalAddressDTO.setTown(strings[12]);
                arrivalAddressDTO.setCountry(strings[13]);
                packageAddressDTO.add(arrivalAddressDTO);
                packageDTO.setAddresses(packageAddressDTO);
                packageDTOFromCsvList.add(packageDTO);
            });
        } catch (CsvException | IOException e) {
            e.printStackTrace();
        }
        return packageDTOFromCsvList;
    }
}
