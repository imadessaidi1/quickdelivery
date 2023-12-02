package com.quickdelivery.abstarct.dto;

import com.quickdelivery.abstarct.parameters.ADDRESS_TYPE;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class AddressDTO {
    private Long id;
    private Timestamp version;
    private String line1;
    private String line2;
    private String town;
    private String zipCode;
    private String country;
    private ADDRESS_TYPE type;
    private BigDecimal latitude;
    private BigDecimal longitude;

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

    @Override
    public String toString(){
        String address="";
        if (line1 != null)
            address+=line1;
        if (line2 != null)
            address+=" "+line2;
        if (zipCode != null)
            address+=" "+zipCode;
        if (town!=null)
            address+=" "+town;
        if (country != null)
            address+=" "+country;
        return address;
    }
}
