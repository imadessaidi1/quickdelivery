package com.quickdelivery.abstarct.dto;

import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;
import com.quickdelivery.abstarct.parameters.GENDER_TYPE;
import com.quickdelivery.abstarct.parameters.PAYMENT_TYPE;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDTO {
    private Long id;
    private Integer version;
    private String type;
    private String firstName;
    private Integer age;
    private GENDER_TYPE sex;
    private Date birthDate;
    private String lastName;
    private String emailAddress;

    private String emailAddressConfirmation;

    private String phoneConfirmation;
    private Boolean emailAddressValidation;
    private String phone;
    private Boolean phoneValidation;
    private Boolean activeAccount;

    private String password;

    private String addressAuto;
    private String deliveryMode;

    private String passwordConfirmation;
    private List<AddressDTO> personalAddress;
    private Map<PAYMENT_TYPE, PaymentDTO> paymentModes = new HashMap<>();
    private Map<DOCUMENT_TYPE, DocumentDTO> document = new HashMap<>();
    private List<PackageReservationDTO> packagesDELIVERED;
    private List<PackageDTO> packagesSent;

    private List<VehicleDTO> vehicles = new ArrayList<>();
    public UserDTO(){}

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

    public List<PackageReservationDTO> getPackagesDELIVERED() {
        return packagesDELIVERED;
    }

    public void setPackagesDELIVERED(List<PackageReservationDTO> packagesDELIVERED) {
        this.packagesDELIVERED = packagesDELIVERED;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<AddressDTO> getPersonalAddress() {
        return personalAddress;
    }

    public void setPersonalAddress(List<AddressDTO> personalAddress) {
        this.personalAddress = personalAddress;
    }

    public List<PackageDTO> getPackagesSent() {
        return packagesSent;
    }

    public void setPackagesSent(List<PackageDTO> packagesSent) {
        this.packagesSent = packagesSent;
    }

    public Map<PAYMENT_TYPE, PaymentDTO> getPaymentModes() {
        return paymentModes;
    }

    public void setPaymentModes(Map<PAYMENT_TYPE, PaymentDTO> paymentModes) {
        this.paymentModes = paymentModes;
    }

    public Map<DOCUMENT_TYPE, DocumentDTO> getDocument() {
        return document;
    }

    public void setDocument(Map<DOCUMENT_TYPE, DocumentDTO> document) {
        this.document = document;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirmation() {
        return passwordConfirmation;
    }

    public void setPasswordConfirmation(String passwordConfirmation) {
        this.passwordConfirmation = passwordConfirmation;
    }

    public String getAddressAuto() {
        return addressAuto;
    }

    public void setAddressAuto(String addressAuto) {
        this.addressAuto = addressAuto;
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = deliveryMode;
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

    public String getEmailAddressConfirmation() {
        return emailAddressConfirmation;
    }

    public void setEmailAddressConfirmation(String emailAddressConfirmation) {
        this.emailAddressConfirmation = emailAddressConfirmation;
    }

    public String getPhoneConfirmation() {
        return phoneConfirmation;
    }

    public void setPhoneConfirmation(String phoneConfirmation) {
        this.phoneConfirmation = phoneConfirmation;
    }

    public List<VehicleDTO> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<VehicleDTO> vehicles) {
        this.vehicles = vehicles;
    }
}
