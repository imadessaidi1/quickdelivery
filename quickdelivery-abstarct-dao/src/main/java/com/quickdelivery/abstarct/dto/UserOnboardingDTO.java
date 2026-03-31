package com.quickdelivery.abstarct.dto;

import java.sql.Timestamp;

public class UserOnboardingDTO {
    private Long userId;
    private String userType;
    private String status;
    private Boolean accountCreated;
    private Boolean profileCompleted;
    private Boolean documentsUploaded;
    private Boolean readyForValidation;
    private Boolean activeAccount;
    private Boolean emailAddressValidation;
    private Timestamp accountCreatedAt;
    private Timestamp profileCompletedAt;
    private Timestamp documentsUploadedAt;
    private Timestamp readyForValidationAt;
    private Timestamp completedAt;
    private Integer currentStep;
    private String resumeToken;
    private String resumeLink;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getAccountCreated() {
        return accountCreated;
    }

    public void setAccountCreated(Boolean accountCreated) {
        this.accountCreated = accountCreated;
    }

    public Boolean getProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(Boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }

    public Boolean getDocumentsUploaded() {
        return documentsUploaded;
    }

    public void setDocumentsUploaded(Boolean documentsUploaded) {
        this.documentsUploaded = documentsUploaded;
    }

    public Boolean getReadyForValidation() {
        return readyForValidation;
    }

    public void setReadyForValidation(Boolean readyForValidation) {
        this.readyForValidation = readyForValidation;
    }

    public Boolean getActiveAccount() {
        return activeAccount;
    }

    public void setActiveAccount(Boolean activeAccount) {
        this.activeAccount = activeAccount;
    }

    public Boolean getEmailAddressValidation() {
        return emailAddressValidation;
    }

    public void setEmailAddressValidation(Boolean emailAddressValidation) {
        this.emailAddressValidation = emailAddressValidation;
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

    public Integer getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Integer currentStep) {
        this.currentStep = currentStep;
    }

    public String getResumeToken() {
        return resumeToken;
    }

    public void setResumeToken(String resumeToken) {
        this.resumeToken = resumeToken;
    }

    public String getResumeLink() {
        return resumeLink;
    }

    public void setResumeLink(String resumeLink) {
        this.resumeLink = resumeLink;
    }
}
