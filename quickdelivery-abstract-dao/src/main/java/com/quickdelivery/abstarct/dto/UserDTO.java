package com.quickdelivery.abstarct.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.sql.Timestamp;
import java.util.Set;

public class UserDTO {
    private Long id;
    private Timestamp version;
    private String type;
    private String firstName;
    private String lastName;
    private String emailAddress;
    private Boolean emailAddressValidation;
    private String phone;
    private Boolean phoneValidation;
    private Boolean activeAccount;
    private Set<AddressDTO> personalAddress;
    private Set<PaymentDTO> paymentDTOS;
    private Set<DocumentDTO> documentDTO;
    private Set<PackageDTO> packagesDELIVERED;
    private Set<PackageDTO> packagesSent;
    private Set<VehicleDTO> vehicleDTOS;
    @JsonCreator
    public UserDTO(){}

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

    public Set<PaymentDTO> getPayments() {
        return paymentDTOS;
    }

    public void setPayments(Set<PaymentDTO> paymentDTOS) {
        this.paymentDTOS = paymentDTOS;
    }

    public Set<DocumentDTO> getDocument() {
        return documentDTO;
    }

    public void setDocument(Set<DocumentDTO> documentDTO) {
        this.documentDTO = documentDTO;
    }

    public Set<PackageDTO> getPackagesDELIVERED() {
        return packagesDELIVERED;
    }

    public void setPackagesDELIVERED(Set<PackageDTO> packagesDELIVERED) {
        this.packagesDELIVERED = packagesDELIVERED;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Set<AddressDTO> getPersonalAddress() {
        return personalAddress;
    }

    public void setPersonalAddress(Set<AddressDTO> personalAddress) {
        this.personalAddress = personalAddress;
    }

    public Set<PaymentDTO> getPaymentDTOS() {
        return paymentDTOS;
    }

    public void setPaymentDTOS(Set<PaymentDTO> paymentDTOS) {
        this.paymentDTOS = paymentDTOS;
    }

    public Set<DocumentDTO> getDocumentDTO() {
        return documentDTO;
    }

    public void setDocumentDTO(Set<DocumentDTO> documentDTO) {
        this.documentDTO = documentDTO;
    }

    public Set<PackageDTO> getPackagesSent() {
        return packagesSent;
    }

    public void setPackagesSent(Set<PackageDTO> packagesSent) {
        this.packagesSent = packagesSent;
    }

    public Set<VehicleDTO> getVehicleDTOS() {
        return vehicleDTOS;
    }

    public void setVehicleDTOS(Set<VehicleDTO> vehicleDTOS) {
        this.vehicleDTOS = vehicleDTOS;
    }
}
