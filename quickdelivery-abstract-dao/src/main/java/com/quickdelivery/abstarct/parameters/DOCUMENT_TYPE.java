package com.quickdelivery.abstarct.parameters;

public enum DOCUMENT_TYPE {
    DRIVER_LICENCE("DRIVER_LICENCE"),
    PACKAGE_PICTURE("PACKAGE_PICTURE"),
    PACKAGE_INVOICE("PACKAGE_INVOICE"),
    GRAY_CARD("GRAY_CARD"),
    INSURANCE("INSURANCE"),
    ID("ID"),
    USER_COMPANY_EXTRACT("USER_COMPANY_EXTRACT"),
    USER_COMPANY_INSURANCE("USER_COMPANY_INSURANCE"),
    RIB("RIB"),
    PACKAGE_QR("PACKAGE_QR"),

    PICTURE("PICTURE"),
    PACKAGE_PDF_LABEL("PACKAGE_PDF_LABEL");
    private String addressType;

    DOCUMENT_TYPE(String addressType){
        this.addressType = addressType;
    }
}
