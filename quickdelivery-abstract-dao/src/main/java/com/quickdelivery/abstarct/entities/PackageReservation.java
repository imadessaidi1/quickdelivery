package com.quickdelivery.abstarct.entities;

import com.quickdelivery.abstarct.parameters.PACKAGE_RESERVATION_STATUS;
import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
public class PackageReservation {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    @Version
    private Timestamp version;
    @Column
    private Timestamp reservationDate;
    @Column
    private String pickUpOTP;
    @Column
    private String deliveryOTP;

    @Column
    private PACKAGE_RESERVATION_STATUS status;
    @ManyToOne
    @JoinColumn(name="deliveryPerson_id")
    private User deliveryPerson;
    @ManyToOne
    @JoinColumn(name="package_id")
    private Package aPackage;

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

    public User getDeliveryPerson() {
        return deliveryPerson;
    }

    public void setDeliveryPerson(User deliveryPerson) {
        this.deliveryPerson = deliveryPerson;
    }

    public Package getaPackage() {
        return aPackage;
    }

    public void setaPackage(Package aPackage) {
        this.aPackage = aPackage;
    }

    public PACKAGE_RESERVATION_STATUS getStatus() {
        return status;
    }

    public void setStatus(PACKAGE_RESERVATION_STATUS status) {
        this.status = status;
    }
}
