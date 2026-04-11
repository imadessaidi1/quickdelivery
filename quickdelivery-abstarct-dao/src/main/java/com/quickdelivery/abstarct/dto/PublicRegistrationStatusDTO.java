package com.quickdelivery.abstarct.dto;

public class PublicRegistrationStatusDTO {
    private boolean emailInUse;
    private boolean resumableOnboarding;
    private String userType;
    private String onboardingStatus;
    private Boolean activeAccount;

    public boolean isEmailInUse() {
        return emailInUse;
    }

    public void setEmailInUse(boolean emailInUse) {
        this.emailInUse = emailInUse;
    }

    public boolean isResumableOnboarding() {
        return resumableOnboarding;
    }

    public void setResumableOnboarding(boolean resumableOnboarding) {
        this.resumableOnboarding = resumableOnboarding;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getOnboardingStatus() {
        return onboardingStatus;
    }

    public void setOnboardingStatus(String onboardingStatus) {
        this.onboardingStatus = onboardingStatus;
    }

    public Boolean getActiveAccount() {
        return activeAccount;
    }

    public void setActiveAccount(Boolean activeAccount) {
        this.activeAccount = activeAccount;
    }
}
