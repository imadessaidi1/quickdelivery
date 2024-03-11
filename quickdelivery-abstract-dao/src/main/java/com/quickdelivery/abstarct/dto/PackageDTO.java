package com.quickdelivery.abstarct.dto;

import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PackageDTO {
    private Long id;
    private Timestamp version;
    private String reference;
    private Timestamp creationDate;

    private Float height;
    private Float width;
    private Float depth;
    private Float weight;
    private String pictureURL;
    private PACKAGE_STATUS status;
    private Double deliveryPrice;
    private Long senderID;

    private String distanceToDestination;
    private List<PackageReservationDTO> packageReservations;
    private List<AddressDTO> addresses;

    private List<DocumentDTO> documentS = new ArrayList<>();
    private BigDecimal lastPositionLatitude;
    private BigDecimal lastPositionLongitude;

    private String fromYou;

    private List<FileDTO> files = new ArrayList<>();
    public PackageDTO(){}
    public Long getId() {
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

    public Double getDeliveryPrice() {
        return deliveryPrice;
    }

    public void setDeliveryPrice(Double deliveryPrice) {
        this.deliveryPrice = deliveryPrice;
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

    public Timestamp getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Timestamp creationDate) {
        this.creationDate = creationDate;
    }

    public List<DocumentDTO> getDocumentS() {
        return documentS;
    }

    public void setDocumentS(List<DocumentDTO> documentS) {
        this.documentS = documentS;
    }

    public BigDecimal getLastPositionLatitude() {
        return lastPositionLatitude;
    }

    public void setLastPositionLatitude(BigDecimal lastPositionLatitude) {
        this.lastPositionLatitude = lastPositionLatitude;
    }

    public BigDecimal getLastPositionLongitude() {
        return lastPositionLongitude;
    }

    public void setLastPositionLongitude(BigDecimal lastPositionLongitude) {
        this.lastPositionLongitude = lastPositionLongitude;
    }

    public List<PackageReservationDTO> getPackageReservations() {
        return packageReservations;
    }

    public void setPackageReservations(List<PackageReservationDTO> packageReservations) {
        this.packageReservations = packageReservations;
    }

    public Float getDepth() {
        return depth;
    }

    public void setDepth(Float depth) {
        this.depth = depth;
    }

    public List<FileDTO> getFiles() {
        return files;
    }

    public void setFiles(List<FileDTO> files) {
        this.files = files;
    }

    public String getDistanceToDestination() {
        return distanceToDestination;
    }

    public void setDistanceToDestination(String distanceToDestination) {
        this.distanceToDestination = distanceToDestination;
    }

    public String getFromYou() {
        return fromYou;
    }

    public void setFromYou(String fromYou) {
        this.fromYou = fromYou;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
