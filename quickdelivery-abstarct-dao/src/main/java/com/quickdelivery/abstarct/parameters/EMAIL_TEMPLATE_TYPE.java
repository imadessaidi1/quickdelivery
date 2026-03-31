package com.quickdelivery.abstarct.parameters;

public enum EMAIL_TEMPLATE_TYPE {
    PACKAGE_CREATION("newpackage-template-thymeleaf.html"),
    PACKAGE_RESERVATION_SENDER("packagereservation-template-thymeleaf.html"),
    PACKAGE_PICKUP_SENDER("packagepickup-template-thymeleaf.html"),
    PACKAGE_PICKUP_DELIVERY("packagepickup-deliveryperson-template-thymeleaf.html"),
    PACKAGE_PICKUP_RECEIVER("packagepickup-recipient-template-thymeleaf.html"),
    PACKAGE_DELIVERY_SENDER("packagedelivery-template-thymeleaf.html"),
    PACKAGE_DELIVERY_RECEIVER("packagedelivery-template-thymeleaf.html"),
    NEW_DELIVERYPERSON_VALIDATION("newdeliveryperson-mailvalidation-template-thymeleaf.html"),
    DELIVERYPERSON_ONBOARDING_RESUME("newdeliveryperson-onboardingresume-template-thymeleaf.html"),
    DELIVERYPERSON_DOCUPDATE_REQUEST("newdeliveryperson-missingdocs-template-thymeleaf.html"),
    DELIVERYPERSON_ACCOUNT_APPROVED("newdeliveryperson-accountapproved-template-thymeleaf.html"),
    PACKAGE_RESERVATION_DELIVERY("packagereservation-deliveryperson-template-thymeleaf.html");

    private String type;

    EMAIL_TEMPLATE_TYPE(String type){
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
