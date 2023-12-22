package com.quickdelivery.abstarct.entities;

import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Package {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    @Version
    private Timestamp version;
    @Column
    private Timestamp creationDate;
    @Column
    private Timestamp reservationDate;
    @Column
    private Float height;
    @Column
    private Float width;
    @Column
    private Float depth;
    @Column
    private Float weight;
    @Column
    private String pictureURL;
    @Column
    @Enumerated(EnumType.STRING)
    private PACKAGE_STATUS status;
    @Column
    private Float deliveryPrice;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "packaged", cascade = CascadeType.ALL)
    private Set<Address> addresses;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "aPackage", cascade = CascadeType.ALL)
    private Set<Document> document = new HashSet<>();
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "aPackage", cascade = CascadeType.ALL)
    private Set<PackageReservation> packageReservations = new HashSet<>();
    public Long getId() {
        return id;
    }
    @ManyToOne
    @JoinColumn(name="sender_id")
    private User sender;

    @Column(precision = 11, scale = 8)
    private BigDecimal lastPositionLatitude;
    @Column(precision = 11, scale = 8)
    private BigDecimal lastPositionLongitude;

    public void setId(Long id) {
        this.id = id;
    }

    public Float getHeight() {
        return height;
    }

    public void setHeight(Float height) {
        this.height = height;
    }

    public Float getWidth() {
        return width;
    }

    public void setWidth(Float width) {
        this.width = width;
    }

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }

    public String getPictureURL() {
        return pictureURL;
    }

    public void setPictureURL(String pictureURL) {
        this.pictureURL = pictureURL;
    }

    public PACKAGE_STATUS getStatus() {
        return status;
    }

    public void setStatus(PACKAGE_STATUS status) {
        this.status = status;
    }

    public Float getDeliveryPrice() {
        return deliveryPrice;
    }

    public void setDeliveryPrice(Float deliveryPrice) {
        this.deliveryPrice = deliveryPrice;
    }

    public Set<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(Set<Address> addresses) {
        this.addresses = addresses;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public Timestamp getVersion() {
        return version;
    }

    public void setVersion(Timestamp version) {
        this.version = version;
    }

    public Timestamp getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Timestamp creationDate) {
        this.creationDate = creationDate;
    }

    public Set<Document> getDocument() {
        return document;
    }

    public void setDocument(Set<Document> document) {
        this.document = document;
    }

    public BigDecimal getLastPositionLatitude() {
        return lastPositionLatitude;
    }

    public void setLastPositionLatitude(BigDecimal lastPositionLatitude) {
        this.lastPositionLatitude = lastPositionLatitude;
    }

    public BigDecimal getLastPositionLongitude() {
        return lastPositionLongitude;
    }

    public void setLastPositionLongitude(BigDecimal lastPositionLongitude) {
        this.lastPositionLongitude = lastPositionLongitude;
    }

    public Timestamp getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(Timestamp reservationDate) {
        this.reservationDate = reservationDate;
    }

    public Set<PackageReservation> getPackageReservations() {
        return packageReservations;
    }

    public void setPackageReservations(Set<PackageReservation> packageReservations) {
        this.packageReservations = packageReservations;
    }

    public Float getDepth() {
        return depth;
    }

    public void setDepth(Float depth) {
        this.depth = depth;
    }
}
