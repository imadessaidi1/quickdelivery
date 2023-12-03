package com.quickdelivery.abstarct.entities;

import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    @Version
    private Timestamp version;
    @Column
    private String type;
    @Column
    private String firstName;
    @Column
    private String lastName;
    @Column
    private String emailAddress;
    @Column
    private Boolean emailAddressValidation;
    @Column
    private String phone;
    @Column
    private Boolean phoneValidation;
    @Column
    private Boolean activeAccount;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "residents", cascade = CascadeType.ALL)
    private Set<Address> personalAddress = new HashSet<>();
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "holderInApp", cascade = CascadeType.ALL)
    private Set<Payment> payments = new HashSet<>();
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.ALL)
    private Set<Document> document = new HashSet<>();
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "deliveryPerson", cascade = CascadeType.ALL)
    private Set<Package> packagesDELIVERED = new HashSet<>();
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "sender", cascade = CascadeType.ALL)
    private Set<Package> packagesSent = new HashSet<>();
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.ALL)
    private Set<Vehicle> vehicles = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Boolean getEmailAddressValidation() {
        return emailAddressValidation;
    }

    public void setEmailAddressValidation(Boolean emailAddressValidation) {
        this.emailAddressValidation = emailAddressValidation;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getPhoneValidation() {
        return phoneValidation;
    }

    public void setPhoneValidation(Boolean phoneValidation) {
        this.phoneValidation = phoneValidation;
    }

    public Boolean getActiveAccount() {
        return activeAccount;
    }

    public void setActiveAccount(Boolean activeAccount) {
        this.activeAccount = activeAccount;
    }

    public Set<Address> getPersonalAddress() {
        return personalAddress;
    }

    public void setPersonalAddress(Set<Address> personalAddress) {
        this.personalAddress = personalAddress;
    }

    public Set<Payment> getPayments() {
        return payments;
    }

    public void setPayments(Set<Payment> payments) {
        this.payments = payments;
    }

    public Set<Document> getDocument() {
        return document;
    }

    public void setDocument(Set<Document> document) {
        this.document = document;
    }

    public Set<Package> getPackagesDELIVERED() {
        return packagesDELIVERED;
    }

    public void setPackagesDELIVERED(Set<Package> packagesDELIVERED) {
        this.packagesDELIVERED = packagesDELIVERED;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Set<Package> getPackagesSent() {
        return packagesSent;
    }

    public void setPackagesSent(Set<Package> packagesSent) {
        this.packagesSent = packagesSent;
    }

    public Set<Vehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(Set<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }
}
