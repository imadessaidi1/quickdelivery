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
    private Integer version;
    @Column
    private Timestamp creationDate;
    @Column
    private String reference;
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
    private Double deliveryPrice;
    @Column
    private String deliverySpeed;
    @Column
    private Boolean insuranceSelected;
    @Column
    private Double declaredValue;
    @Column
    private String distanceToDestination;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "packaged", cascade = CascadeType.ALL)
    private Set<Address> addresses = new HashSet<>();

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
    @Column
    private Boolean guestMode;
    @Column
    private String guestAccessToken;

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

    public Double getDeliveryPrice() {
        return deliveryPrice;
    }

    public void setDeliveryPrice(Double deliveryPrice) {
        this.deliveryPrice = deliveryPrice;
    }

    public String getDeliverySpeed() {
        return deliverySpeed;
    }

    public void setDeliverySpeed(String deliverySpeed) {
        this.deliverySpeed = deliverySpeed;
    }

    public Boolean getInsuranceSelected() {
        return insuranceSelected;
    }

    public void setInsuranceSelected(Boolean insuranceSelected) {
        this.insuranceSelected = insuranceSelected;
    }

    public Double getDeclaredValue() {
        return declaredValue;
    }

    public void setDeclaredValue(Double declaredValue) {
        this.declaredValue = declaredValue;
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

    public Boolean getGuestMode() {
        return guestMode;
    }

    public void setGuestMode(Boolean guestMode) {
        this.guestMode = guestMode;
    }

    public String getGuestAccessToken() {
        return guestAccessToken;
    }

    public void setGuestAccessToken(String guestAccessToken) {
        this.guestAccessToken = guestAccessToken;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
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

    public String getDistanceToDestination() {
        return distanceToDestination;
    }

    public void setDistanceToDestination(String distanceToDestination) {
        this.distanceToDestination = distanceToDestination;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
