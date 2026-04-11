package com.quickdelivery.abstarct.entities;

import com.quickdelivery.abstarct.parameters.DELIVERY_ROUTE_STATUS;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
public class DeliveryRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Version
    private Integer version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_person_id", nullable = false)
    private User deliveryPerson;
    @Column
    @Enumerated(EnumType.STRING)
    private DELIVERY_ROUTE_STATUS status;
    @Column
    private Double startLatitude;
    @Column
    private Double startLongitude;
    @Column
    private Double endLatitude;
    @Column
    private Double endLongitude;
    @Column
    private Double totalDistanceMeters;
    @Column
    private Double estimatedDurationMinutes;
    @Column
    private Double totalCourierPayout;
    @Column
    private Double totalWeightKg;
    @Column
    private Double totalVolumeCm3;
    @Column
    private Timestamp createdAt;
    @Column
    private Timestamp startedAt;
    @Column
    private Timestamp completedAt;
    @OneToMany(mappedBy = "deliveryRoute", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DeliveryRouteStop> stops = new LinkedHashSet<>();

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

    public User getDeliveryPerson() {
        return deliveryPerson;
    }

    public void setDeliveryPerson(User deliveryPerson) {
        this.deliveryPerson = deliveryPerson;
    }

    public DELIVERY_ROUTE_STATUS getStatus() {
        return status;
    }

    public void setStatus(DELIVERY_ROUTE_STATUS status) {
        this.status = status;
    }

    public Double getStartLatitude() {
        return startLatitude;
    }

    public void setStartLatitude(Double startLatitude) {
        this.startLatitude = startLatitude;
    }

    public Double getStartLongitude() {
        return startLongitude;
    }

    public void setStartLongitude(Double startLongitude) {
        this.startLongitude = startLongitude;
    }

    public Double getEndLatitude() {
        return endLatitude;
    }

    public void setEndLatitude(Double endLatitude) {
        this.endLatitude = endLatitude;
    }

    public Double getEndLongitude() {
        return endLongitude;
    }

    public void setEndLongitude(Double endLongitude) {
        this.endLongitude = endLongitude;
    }

    public Double getTotalDistanceMeters() {
        return totalDistanceMeters;
    }

    public void setTotalDistanceMeters(Double totalDistanceMeters) {
        this.totalDistanceMeters = totalDistanceMeters;
    }

    public Double getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Double estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public Double getTotalCourierPayout() {
        return totalCourierPayout;
    }

    public void setTotalCourierPayout(Double totalCourierPayout) {
        this.totalCourierPayout = totalCourierPayout;
    }

    public Double getTotalWeightKg() {
        return totalWeightKg;
    }

    public void setTotalWeightKg(Double totalWeightKg) {
        this.totalWeightKg = totalWeightKg;
    }

    public Double getTotalVolumeCm3() {
        return totalVolumeCm3;
    }

    public void setTotalVolumeCm3(Double totalVolumeCm3) {
        this.totalVolumeCm3 = totalVolumeCm3;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

    public Set<DeliveryRouteStop> getStops() {
        return stops;
    }

    public void setStops(Set<DeliveryRouteStop> stops) {
        this.stops = stops;
    }
}
