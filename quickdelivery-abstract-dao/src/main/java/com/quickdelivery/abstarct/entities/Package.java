package com.quickdelivery.abstarct.entities;

import com.quickdelivery.abstarct.parameters.PACKAGE_STATUS;
import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.Set;

@Entity
public class Package {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    @Version
    private Timestamp version;
    @Column
    private Float height;
    @Column
    private Float width;
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
    public Long getId() {
        return id;
    }
    @ManyToOne
    @JoinColumn(name="deliveryPerson_id")
    private User deliveryPerson;
    @ManyToOne
    @JoinColumn(name="sender_id")
    private User sender;

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

    public User getDeliveryPerson() {
        return deliveryPerson;
    }

    public void setDeliveryPerson(User deliveryPerson) {
        this.deliveryPerson = deliveryPerson;
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
}
