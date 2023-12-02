package com.quickdelivery.abstarct.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.sql.Timestamp;
import java.util.Set;

public class VehicleDTO {
    private Long id;
    private Timestamp version;
    private String type;
    private String brand;
    private String model;
    private String energyType;
    private Set<DocumentDTO> documentDTO;
    @JsonCreator
    public VehicleDTO(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public Set<DocumentDTO> getDocument() {
        return documentDTO;
    }

    public void setDocument(Set<DocumentDTO> documentDTO) {
        this.documentDTO = documentDTO;
    }

    public Timestamp getVersion() {
        return version;
    }

    public void setVersion(Timestamp version) {
        this.version = version;
    }
}
