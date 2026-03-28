package com.quickdelivery.abstarct.entities;

import com.quickdelivery.abstarct.parameters.COURIER_PAYOUT_STATUS;
import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
public class CourierPayout {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Integer version;

    @Column
    private Timestamp createdAt;

    @Column
    private Timestamp approvedAt;

    @Column
    private Timestamp paidAt;

    @Column
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column
    private COURIER_PAYOUT_STATUS status;

    @Column
    private String paymentReference;

    @Column
    private String batchReference;

    @ManyToOne
    @JoinColumn(name = "delivery_person_id")
    private User deliveryPerson;

    @ManyToOne
    @JoinColumn(name = "package_id")
    private Package aPackage;

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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Timestamp approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Timestamp getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Timestamp paidAt) {
        this.paidAt = paidAt;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public COURIER_PAYOUT_STATUS getStatus() {
        return status;
    }

    public void setStatus(COURIER_PAYOUT_STATUS status) {
        this.status = status;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getBatchReference() {
        return batchReference;
    }

    public void setBatchReference(String batchReference) {
        this.batchReference = batchReference;
    }

    public User getDeliveryPerson() {
        return deliveryPerson;
    }

    public void setDeliveryPerson(User deliveryPerson) {
        this.deliveryPerson = deliveryPerson;
    }

    public Package getaPackage() {
        return aPackage;
    }

    public void setaPackage(Package aPackage) {
        this.aPackage = aPackage;
    }
}
