package com.quickdelivery.abstarct.parameters;

public enum PACKAGE_STATUS {
    PAYMENTPENDING("PAYMENTPENDING"),
    NEW("NEW"),
    PICKEDUP("PICKEDUP"),
    RESERVED("RESERVED"),
    INDELIVERY("INDELIVERY"),
    PICKUP_FAILED("PICKUP_FAILED"),
    RELAY_DROPOFF_REQUIRED("RELAY_DROPOFF_REQUIRED"),
    DELIVERED("DELIVERED");
    private String status;

    PACKAGE_STATUS(String status){
        this.status = status;
    }
}
