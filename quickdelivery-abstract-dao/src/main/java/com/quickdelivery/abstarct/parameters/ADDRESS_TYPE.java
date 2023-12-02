package com.quickdelivery.abstarct.parameters;

public enum ADDRESS_TYPE {
    RESIDENCE("RESIDENCE"),
    DEPARTURE("DEPARTURE"),
    ARRIVAL("ARRIVAL");
    private String addressType;

    ADDRESS_TYPE(String addressType){
        this.addressType = addressType;
    }
}
