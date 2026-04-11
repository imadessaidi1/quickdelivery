package com.quickdelivery.abstarct.dto;

import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;

import java.util.HashMap;
import java.util.Map;

public class VehicleDTO {
    private Long id;
    private Integer version;
    private String registrationNumber;
    private String brand;
    private String model;
    private String type;
    private String energyType;
    private Map<DOCUMENT_TYPE, DocumentDTO> vehicleDocuments = new HashMap<>();
    public VehicleDTO(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getEnergyType() {
        return energyType;
    }

    public void setEnergyType(String energyType) {
        this.energyType = energyType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public Map<DOCUMENT_TYPE, DocumentDTO> getVehicleDocuments() {
        return vehicleDocuments;
    }

    public void setVehicleDocuments(Map<DOCUMENT_TYPE, DocumentDTO> vehicleDocuments) {
        this.vehicleDocuments = vehicleDocuments;
    }
}
