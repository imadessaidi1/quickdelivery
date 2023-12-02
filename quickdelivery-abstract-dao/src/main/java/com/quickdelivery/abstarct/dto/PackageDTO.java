package com.quickdelivery.abstarct.dto;

import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;

import java.sql.Timestamp;
import java.util.List;

public class PackageDTO {
    private Long id;
    private Timestamp version;
    private Float height;
    private Float width;
    private Float weight;
    private String pictureURL;
    private PACKAGE_STATUS status;
    private Float deliveryPrice;
    private Long senderID;
    private Long deliveryPersonID;
    private List<AddressDTO> addresses;
    public PackageDTO(){}
    private Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Float getHeight() {
        return height;
    }

    public void setHeight(Float height) {
        this.height = height;
    }

    public Float getWidth() {
        return width;
    }

    public void setWidth(Float width) {
        this.width = width;
    }

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }

    public String getPictureURL() {
        return pictureURL;
    }

    public void setPictureURL(String pictureURL) {
        this.pictureURL = pictureURL;
    }

    public PACKAGE_STATUS getStatus() {
        return status;
    }

    public void setStatus(PACKAGE_STATUS status) {
        this.status = status;
    }

    public Float getDeliveryPrice() {
        return deliveryPrice;
    }

    public void setDeliveryPrice(Float deliveryPrice) {
        this.deliveryPrice = deliveryPrice;
    }

    public Long getDeliveryPersonID() {
        return deliveryPersonID;
    }

    public void setDeliveryPersonID(Long deliveryPersonID) {
        this.deliveryPersonID = deliveryPersonID;
    }

    public List<AddressDTO> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<AddressDTO> addresses) {
        this.addresses = addresses;
    }

    public Timestamp getVersion() {
        return version;
    }

    public void setVersion(Timestamp version) {
        this.version = version;
    }

    public Long getSenderID() {
        return senderID;
    }

    public void setSenderID(Long senderID) {
        this.senderID = senderID;
    }
}
