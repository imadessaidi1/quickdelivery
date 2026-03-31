package com.quickdelivery.abstarct.entities;

import com.quickdelivery.abstarct.parameters.USER_ONBOARDING_STATUS;
import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
public class UserOnboarding {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Version
    private Integer version;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private USER_ONBOARDING_STATUS status;

    @Column
    private Timestamp accountCreatedAt;

    @Column
    private Timestamp profileCompletedAt;

    @Column
    private Timestamp documentsUploadedAt;

    @Column
    private Timestamp readyForValidationAt;

    @Column
    private Timestamp completedAt;

    @Column(nullable = false)
    private Timestamp lastUpdatedAt;

    @Column
    private Integer currentStep;

    @Column
    private String lastErrorCode;

    @Column(length = 2000)
    private String lastErrorMessage;

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

    public USER_ONBOARDING_STATUS getStatus() {
        return status;
    }

    public void setStatus(USER_ONBOARDING_STATUS status) {
        this.status = status;
    }

    public Timestamp getAccountCreatedAt() {
        return accountCreatedAt;
    }

    public void setAccountCreatedAt(Timestamp accountCreatedAt) {
        this.accountCreatedAt = accountCreatedAt;
    }

    public Timestamp getProfileCompletedAt() {
        return profileCompletedAt;
    }

    public void setProfileCompletedAt(Timestamp profileCompletedAt) {
        this.profileCompletedAt = profileCompletedAt;
    }

    public Timestamp getDocumentsUploadedAt() {
        return documentsUploadedAt;
    }

    public void setDocumentsUploadedAt(Timestamp documentsUploadedAt) {
        this.documentsUploadedAt = documentsUploadedAt;
    }

    public Timestamp getReadyForValidationAt() {
        return readyForValidationAt;
    }

    public void setReadyForValidationAt(Timestamp readyForValidationAt) {
        this.readyForValidationAt = readyForValidationAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

    public Timestamp getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Timestamp lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public Integer getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Integer currentStep) {
        this.currentStep = currentStep;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public void setLastErrorCode(String lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }
}
