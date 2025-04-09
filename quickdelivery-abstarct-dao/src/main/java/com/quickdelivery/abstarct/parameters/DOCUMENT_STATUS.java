package com.quickdelivery.abstarct.parameters;

public enum DOCUMENT_STATUS {
    PENDING_VALIDATION("PENDING_VALIDATION"),
    ACCEPTED("ACCEPTED"),
    REJECTED("REJECTED");
    private String status;

    DOCUMENT_STATUS(String status){
        this.status = status;
    }
}
