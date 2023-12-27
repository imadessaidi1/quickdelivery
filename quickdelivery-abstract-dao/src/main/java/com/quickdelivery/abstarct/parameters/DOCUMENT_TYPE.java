package com.quickdelivery.abstarct.parameters;

public enum DOCUMENT_TYPE {
    DRIVER_LICENCE("DRIVER_LICENCE"),
    PACKAGE_PICTURE("PACKAGE_PICTURE"),
    PACKAGE_INVOICE("PACKAGE_INVOICE"),
    VEHICLE_GRY_CARD("VEHICLE_GRY_CARD"),
    USER_ID("USER_ID"),
    PACKAGE_QR("PACKAGE_QR"),
    PACKAGE_PDF_LABEL("PACKAGE_PDF_LABEL");
    private String addressType;

    DOCUMENT_TYPE(String addressType){
        this.addressType = addressType;
    }
}
