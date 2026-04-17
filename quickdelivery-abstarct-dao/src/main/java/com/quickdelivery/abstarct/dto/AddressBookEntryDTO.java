package com.quickdelivery.abstarct.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class AddressBookEntryDTO {
    private Long id;
    private Integer version;
    private Long ownerUserId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String line1;
    private String line2;
    private String town;
    private String zipCode;
    private String country;
    private Integer floor;
    private Boolean hasElevator;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String addressAuto;
    private String displayLabel;
    private Boolean active;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp lastUsedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public Boolean getHasElevator() {
        return hasElevator;
    }

    public void setHasElevator(Boolean hasElevator) {
        this.hasElevator = hasElevator;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getAddressAuto() {
        return addressAuto;
    }

    public void setAddressAuto(String addressAuto) {
        this.addressAuto = addressAuto;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public void setDisplayLabel(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Timestamp getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Timestamp lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
