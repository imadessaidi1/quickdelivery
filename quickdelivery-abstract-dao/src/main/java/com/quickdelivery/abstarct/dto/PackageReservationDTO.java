package com.quickdelivery.abstarct.dto;

import com.quickdelivery.abstarct.entities.Package;
import com.quickdelivery.abstarct.entities.User;
import jakarta.persistence.*;

import java.sql.Timestamp;

public class PackageReservationDTO {
    private Long id;
    private Timestamp version;
    private Timestamp reservationDate;
    private String pickUpOTP;
    private String deliveryOTP;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Timestamp getVersion() {
        return version;
    }

    public void setVersion(Timestamp version) {
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
}
