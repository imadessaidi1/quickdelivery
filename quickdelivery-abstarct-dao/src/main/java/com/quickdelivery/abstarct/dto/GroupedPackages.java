package com.quickdelivery.abstarct.dto;

import java.util.List;

public class GroupedPackages {
    private AddressDTO addressDTO;
    private List<PackageDTO> packageDTOList;

    public AddressDTO getAddressDTO() {
        return addressDTO;
    }

    public void setAddressDTO(AddressDTO addressDTO) {
        this.addressDTO = addressDTO;
    }

    public List<PackageDTO> getPackageDTOList() {
        return packageDTOList;
    }

    public void setPackageDTOList(List<PackageDTO> packageDTOList) {
        this.packageDTOList = packageDTOList;
    }
}
