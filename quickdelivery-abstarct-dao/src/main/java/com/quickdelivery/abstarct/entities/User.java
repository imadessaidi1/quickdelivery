package com.quickdelivery.abstarct.entities;

import com.quickdelivery.abstarct.parameters.GENDER_TYPE;
import jakarta.persistence.*;

import java.sql.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    @Version
    private Integer version;
    @Column
    private String type;
    @Column
    private String firstName;
    @Column
    private String lastName;
    @Column
    private Integer age;
    @Column
    private Date birthDate;
    @Column
    private GENDER_TYPE sex;
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
    @Column
    private String password;
    @Column
    private String deliveryMode;
    @Column(length = 10)
    private String preferredLocale;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "residents", cascade = CascadeType.ALL)
    private Set<Address> personalAddress = new HashSet<>();
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "holderInApp", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Payment> payments = new HashSet<>();
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.ALL)
    private Set<Document> document = new HashSet<>();
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "deliveryPerson", cascade = CascadeType.ALL)
    private Set<PackageReservation> packagesDELIVERED = new HashSet<>();
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

    public Set<PackageReservation> getPackagesDELIVERED() {
        return packagesDELIVERED;
    }

    public void setPackagesDELIVERED(Set<PackageReservation> packagesDELIVERED) {
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

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public GENDER_TYPE getSex() {
        return sex;
    }

    public void setSex(GENDER_TYPE sex) {
        this.sex = sex;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public String getPreferredLocale() {
        return preferredLocale;
    }

    public void setPreferredLocale(String preferredLocale) {
        this.preferredLocale = preferredLocale;
    }
}
