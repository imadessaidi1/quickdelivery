package com.quickdelivery.abstarct.parameters;

public enum PACKAGE_RESERVATION_STATUS {
    ONGOING("ONGOING"),
    FINISHED("FINISHED"),
    CANCELED("CANCELED");
    private String status;

    PACKAGE_RESERVATION_STATUS(String status){
        this.status = status;
    }
}
