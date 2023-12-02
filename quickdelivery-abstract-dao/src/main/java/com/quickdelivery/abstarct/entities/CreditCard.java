package com.quickdelivery.abstarct.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class CreditCard extends Payment {
    @Column
    private String number;
    @Column
    private String cryptogram;
    @Column
    private Integer expiryMonth;
    @Column
    private Integer expiryYear;
    @Column
    private String holderName;

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getCryptogram() {
        return cryptogram;
    }

    public void setCryptogram(String cryptogram) {
        this.cryptogram = cryptogram;
    }

    public Integer getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(Integer expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public Integer getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(Integer expiryYear) {
        this.expiryYear = expiryYear;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String holderName) {
        this.holderName = holderName;
    }

}
