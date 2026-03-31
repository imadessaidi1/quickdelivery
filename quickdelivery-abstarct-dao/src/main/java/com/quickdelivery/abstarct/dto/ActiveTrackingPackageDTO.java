package com.quickdelivery.abstarct.dto;

public class ActiveTrackingPackageDTO {
    private String packageReference;
    private String status;

    public String getPackageReference() {
        return packageReference;
    }

    public void setPackageReference(String packageReference) {
        this.packageReference = packageReference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
