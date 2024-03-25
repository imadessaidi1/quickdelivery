package com.quickdelivery.abstarct.dto;

import com.quickdelivery.abstarct.parameters.PAYMENT_TYPE;

import java.sql.Timestamp;

public class PaymentDTO {
    private Long id;
    private Timestamp version;
    private PAYMENT_TYPE paymentType;
    private Boolean validated;
    private String holderNam;
    private String cardNumber;
    private String expiryDate;
    private String cvv;

    private String iban;
    private String bic;
    public PaymentDTO(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getValidated() {
        return validated;
    }

    public void setValidated(Boolean validated) {
        this.validated = validated;
    }

    public Timestamp getVersion() {
        return version;
    }

    public void setVersion(Timestamp version) {
        this.version = version;
    }

    public PAYMENT_TYPE getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PAYMENT_TYPE paymentType) {
        this.paymentType = paymentType;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

    public String getHolderNam() {
        return holderNam;
    }

    public void setHolderNam(String holderNam) {
        this.holderNam = holderNam;
    }
}
