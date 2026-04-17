package com.quickdelivery.abstarct.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "customer_address_book_entry", indexes = {
        @Index(name = "idx_address_book_owner_updated_at", columnList = "owner_user_id, updated_at"),
        @Index(name = "idx_address_book_owner_email", columnList = "owner_user_id, email"),
        @Index(name = "idx_address_book_owner_name", columnList = "owner_user_id, last_name, first_name")
})
public class CustomerAddressBookEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Version
    private Integer version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
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
    private Boolean hasElevator;
    @Column(precision = 11, scale = 8)
    private BigDecimal latitude;
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;
    @Column(length = 1024)
    private String addressAuto;
    @Column(nullable = false)
    private Boolean active;
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;
    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;
    @Column(name = "last_used_at")
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

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
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
