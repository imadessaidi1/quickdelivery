package com.quickdelivery.abstarct.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.sql.Timestamp;
import java.util.List;

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
    private List<AddressDTO> personalAddress;
    private List<PaymentDTO> paymentDTOS;
    private List<DocumentDTO> documentDTO;
    private List<PackageReservationDTO> packagesDELIVERED;
    private List<PackageDTO> packagesSent;
    private List<VehicleDTO> vehicleDTOS;
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

    public List<PaymentDTO> getPayments() {
        return paymentDTOS;
    }

    public void setPayments(List<PaymentDTO> paymentDTOS) {
        this.paymentDTOS = paymentDTOS;
    }

    public List<DocumentDTO> getDocument() {
        return documentDTO;
    }

    public void setDocument(List<DocumentDTO> documentDTO) {
        this.documentDTO = documentDTO;
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

    public List<PaymentDTO> getPaymentDTOS() {
        return paymentDTOS;
    }

    public void setPaymentDTOS(List<PaymentDTO> paymentDTOS) {
        this.paymentDTOS = paymentDTOS;
    }

    public List<DocumentDTO> getDocumentDTO() {
        return documentDTO;
    }

    public void setDocumentDTO(List<DocumentDTO> documentDTO) {
        this.documentDTO = documentDTO;
    }

    public List<PackageDTO> getPackagesSent() {
        return packagesSent;
    }

    public void setPackagesSent(List<PackageDTO> packagesSent) {
        this.packagesSent = packagesSent;
    }

    public List<VehicleDTO> getVehicleDTOS() {
        return vehicleDTOS;
    }

    public void setVehicleDTOS(List<VehicleDTO> vehicleDTOS) {
        this.vehicleDTOS = vehicleDTOS;
    }
}
