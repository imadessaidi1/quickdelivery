package com.quickdelivery.abstarct.entities;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Vehicle {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    @Version
    private Integer version;
    @Column
    private String type;
    @Column
    private String brand;
    @Column
    private String model;
    @Column
    private String registrationNumber;
    @Column
    private String energyType;
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
    private Set<Document> document = new HashSet<>();
    @ManyToOne
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getEnergyType() {
        return energyType;
    }

    public void setEnergyType(String energyType) {
        this.energyType = energyType;
    }

    public Set<Document> getDocument() {
        return document;
    }

    public void setDocument(Set<Document> document) {
        this.document = document;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }
}
