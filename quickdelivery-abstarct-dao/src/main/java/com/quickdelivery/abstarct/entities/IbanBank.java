package com.quickdelivery.abstarct.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class IbanBank extends Payment {

    @Column
    private String iban;

    @Column
    private String bic;

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
}
