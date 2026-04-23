package com.quickdelivery.abstarct.entities;

import com.quickdelivery.abstarct.parameters.USER_PENALTY_STATUS;
import com.quickdelivery.abstarct.parameters.USER_PENALTY_TYPE;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

import java.sql.Timestamp;

@Entity
public class UserPenalty {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Version
    private Integer version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    private Package aPackage;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private USER_PENALTY_TYPE type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private USER_PENALTY_STATUS status;
    @Column(length = 500)
    private String reason;
    @Column(length = 4000)
    private String detailsJson;
    @Column
    private Double financialPenaltyAmount;
    @Column(length = 3)
    private String currency;
    @Column(nullable = false)
    private Timestamp createdAt;
    @Column
    private Timestamp reviewedAt;

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Package getaPackage() {
        return aPackage;
    }

    public void setaPackage(Package aPackage) {
        this.aPackage = aPackage;
    }

    public USER_PENALTY_TYPE getType() {
        return type;
    }

    public void setType(USER_PENALTY_TYPE type) {
        this.type = type;
    }

    public USER_PENALTY_STATUS getStatus() {
        return status;
    }

    public void setStatus(USER_PENALTY_STATUS status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }

    public Double getFinancialPenaltyAmount() {
        return financialPenaltyAmount;
    }

    public void setFinancialPenaltyAmount(Double financialPenaltyAmount) {
        this.financialPenaltyAmount = financialPenaltyAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Timestamp reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
