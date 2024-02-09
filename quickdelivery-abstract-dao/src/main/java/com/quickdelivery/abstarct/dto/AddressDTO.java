package com.quickdelivery.abstarct.dto;

import com.quickdelivery.abstarct.parameters.ADDRESS_TYPE;
import jakarta.persistence.Column;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class AddressDTO {
    private Long id;
    private Timestamp version;
    private String firstName;
    private String lastName;
    private String line1;
    private String line2;
    private String town;
    private String zipCode;
    private String country;
    private Integer floor;
    private Timestamp dateTime;
    private ADDRESS_TYPE type;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String email;
    private String phone;

    public AddressDTO(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public ADDRESS_TYPE getType() {
        return type;
    }

    public void setType(ADDRESS_TYPE type) {
        this.type = type;
    }
    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Timestamp getVersion() {
        return version;
    }

    public void setVersion(Timestamp version) {
        this.version = version;
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

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public Timestamp getDateTime() {
        return dateTime;
    }

    public void setDateTime(Timestamp dateTime) {
        this.dateTime = dateTime;
    }

    @Override
    public String toString(){
        StringBuilder address= new StringBuilder();
        if (line1 != null)
            address.append(line1);
        if (line2 != null)
            address.append(" ").append(line2);
        if (zipCode != null)
            address.append(" ").append(zipCode);
        if (town!=null)
            address.append(" ").append(town);
        if (country != null)
            address.append(" ").append(country);
        return address.toString();
    }
    public String formatedtoString(){
        StringBuilder address= new StringBuilder();
        if (line1 != null)
            address.append(line1);
        if (line2 != null)
            address.append("\n").append(line2);
        if (zipCode != null)
            address.append("\n").append(zipCode);
        if (town!=null)
            address.append("\n").append(town);
        if (country != null)
            address.append("\n").append(country);
        return address.toString();
    }
}
