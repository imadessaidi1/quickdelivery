package com.quickdelivery.abstarct.dto;

import java.sql.Timestamp;

public class PaymentDTO {
    private Long id;
    private Timestamp version;
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
}
