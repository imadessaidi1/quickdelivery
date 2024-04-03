package com.quickdelivery.abstarct.entities;

import com.quickdelivery.abstarct.parameters.PAYMENT_TYPE;
import jakarta.persistence.*;

@Entity
public class Payment {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    @Version
    private Integer version;
    @Column
    protected Boolean validated;
    @Column
    private PAYMENT_TYPE paymentType;
    @ManyToOne
    @JoinColumn(name="holderInApp_id", nullable=false)
    private User holderInApp;

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

    public User getHolderInApp() {
        return holderInApp;
    }

    public void setHolderInApp(User holderInApp) {
        this.holderInApp = holderInApp;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public PAYMENT_TYPE getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PAYMENT_TYPE paymentType) {
        this.paymentType = paymentType;
    }
}
