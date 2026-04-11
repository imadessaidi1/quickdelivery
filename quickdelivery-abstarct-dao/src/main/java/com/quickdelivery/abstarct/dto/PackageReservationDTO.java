package com.quickdelivery.abstarct.dto;

import com.quickdelivery.abstarct.parameters.PACKAGE_RESERVATION_STATUS;

import java.sql.Timestamp;

public class PackageReservationDTO {
    private Long id;
    private Integer version;
    private Timestamp reservationDate;
    private String pickUpOTP;
    private String deliveryOTP;
    private PACKAGE_RESERVATION_STATUS status;
    private Long deliveryPersonId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Timestamp getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(Timestamp reservationDate) {
        this.reservationDate = reservationDate;
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

    public PACKAGE_RESERVATION_STATUS getStatus() {
        return status;
    }

    public void setStatus(PACKAGE_RESERVATION_STATUS status) {
        this.status = status;
    }

    public Long getDeliveryPersonId() {
        return deliveryPersonId;
    }

    public void setDeliveryPersonId(Long deliveryPersonId) {
        this.deliveryPersonId = deliveryPersonId;
    }
}
