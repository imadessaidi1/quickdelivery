package com.quickdelivery.abstarct.dto;

import com.quickdelivery.abstarct.parameters.PACKAGE_RESERVATION_STATUS;
import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;

public class DeliveryReservationContextDTO {
    private Long packageId;
    private String packageReference;
    private PACKAGE_STATUS packageStatus;
    private PACKAGE_RESERVATION_STATUS reservationStatus;
    private String pickUpOTP;
    private String deliveryOTP;

    public Long getPackageId() {
        return packageId;
    }

    public void setPackageId(Long packageId) {
        this.packageId = packageId;
    }

    public String getPackageReference() {
        return packageReference;
    }

    public void setPackageReference(String packageReference) {
        this.packageReference = packageReference;
    }

    public PACKAGE_STATUS getPackageStatus() {
        return packageStatus;
    }

    public void setPackageStatus(PACKAGE_STATUS packageStatus) {
        this.packageStatus = packageStatus;
    }

    public PACKAGE_RESERVATION_STATUS getReservationStatus() {
        return reservationStatus;
    }

    public void setReservationStatus(PACKAGE_RESERVATION_STATUS reservationStatus) {
        this.reservationStatus = reservationStatus;
    }

    public String getPickUpOTP() {
        return pickUpOTP;
    }

    public void setPickUpOTP(String pickUpOTP) {
        this.pickUpOTP = pickUpOTP;
    }

    public String getDeliveryOTP() {
        return deliveryOTP;
    }

    public void setDeliveryOTP(String deliveryOTP) {
        this.deliveryOTP = deliveryOTP;
    }
}
