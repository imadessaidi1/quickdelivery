package com.quickdelivery.abstarct.parameters;

public enum GENDER_TYPE {
    MAL("MAL"),
    OTHER("OTHER"),
    FEMALE("FEMALE");
    private String gender;

    GENDER_TYPE(String gender){
        this.gender = gender;
    }
}
