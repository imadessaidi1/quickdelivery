package com.quickdelivery.abstarct.parameters;

public enum CHECK_STATUS {
    OK("OK"),
    KO("KO");
    private String chackStatus;

    CHECK_STATUS(String chackStatus){
        this.chackStatus = chackStatus;
    }
}
