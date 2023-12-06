package com.quickdelivery.abstarct.parameters;

public enum PACKAGE_STATUS {
    NEW("NEW"),
    PICKEDUP("PICKEDUP"),
    RESERVED("RESERVED"),
    INDELIVERY("INDELIVERY"),
    DELIVERED("DELIVERED");
    private String status;

    PACKAGE_STATUS(String status){
        this.status = status;
    }
}
