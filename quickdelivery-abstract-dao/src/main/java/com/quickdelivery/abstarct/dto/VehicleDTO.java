package com.quickdelivery.abstarct.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.sql.Timestamp;
import java.util.Set;

public class VehicleDTO {
    private Long id;
    private Timestamp version;
    private String registrationNumber;
    private String brand;
    private String model;
    private String energyType;
    private Set<DocumentDTO> vehicleDocuments;
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

    public Timestamp getVersion() {
        return version;
    }

    public void setVersion(Timestamp version) {
        this.version = version;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public Set<DocumentDTO> getVehicleDocuments() {
        return vehicleDocuments;
    }

    public void setVehicleDocuments(Set<DocumentDTO> vehicleDocuments) {
        this.vehicleDocuments = vehicleDocuments;
    }
}
