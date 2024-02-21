package com.quickdelivery.abstarct.dto;

import com.quickdelivery.abstarct.parameters.PAYMENT_TYPE;

import java.sql.Timestamp;

public class PaymentDTO {
    private Long id;
    private Timestamp version;

    private PAYMENT_TYPE paymentType;
    protected Boolean validated;
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
}
