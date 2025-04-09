package com.quickdelivery.abstarct.entities;

import com.quickdelivery.abstarct.parameters.ADDRESS_TYPE;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
public class Address {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    @Version
    private Integer version;
    @Column
    private String firstName;
    @Column
    private String lastName;
    @Column
    private String email;
    @Column
    private String phone;
    @Column
    private String line1;
    @Column
    private String line2;
    @Column
    private String town;
    @Column
    private String zipCode;
    @Column
    private String country;
    @Column
    private Integer floor;
    @Column
    private Timestamp dateTime;
    @Column
    @Enumerated(EnumType.STRING)
    private ADDRESS_TYPE type;
    @Column(precision = 11, scale = 8)
    private BigDecimal latitude;
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;
    @ManyToOne
    @JoinColumn(name="residents_user_id")
    private User residents;
    @ManyToOne
    @JoinColumn(name="package_id")
    private Package packaged;

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

    public User getResidents() {
        return residents;
    }

    public void setResidents(User residents) {
        this.residents = residents;
    }

    public ADDRESS_TYPE getType() {
        return type;
    }

    public void setType(ADDRESS_TYPE type) {
        this.type = type;
    }

    public Package getPackaged() {
        return packaged;
    }

    public void setPackaged(Package packaged) {
        this.packaged = packaged;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
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

    public Timestamp getDateTime() {
        return dateTime;
    }

    public void setDateTime(Timestamp dateTime) {
        this.dateTime = dateTime;
    }

    public String formatedtoString(){
        StringBuilder address= new StringBuilder();
        if(firstName != null)
            address.append("\n").append(firstName);
        if(lastName != null)
            address.append(" "+lastName);
        if (line1 != null)
            address.append("\n").append(line1);
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
