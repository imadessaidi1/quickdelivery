package com.quickdelivery.abstarct.entities;

import com.quickdelivery.abstarct.parameters.DELIVERY_ROUTE_STOP_KIND;
import com.quickdelivery.abstarct.parameters.DELIVERY_ROUTE_STOP_STATUS;
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
import jakarta.persistence.Version;

import java.sql.Timestamp;

@Entity
public class DeliveryRouteStop {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Version
    private Integer version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_route_id", nullable = false)
    private DeliveryRoute deliveryRoute;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private Package aPackage;
    @Column
    @Enumerated(EnumType.STRING)
    private DELIVERY_ROUTE_STOP_KIND kind;
    @Column
    @Enumerated(EnumType.STRING)
    private DELIVERY_ROUTE_STOP_STATUS status;
    @Column
    private Integer stopOrder;
    @Column
    private Double latitude;
    @Column
    private Double longitude;
    @Column(length = 512)
    private String addressLabel;
    @Column
    private Double plannedWeightKg;
    @Column
    private Double plannedVolumeCm3;
    @Column
    private Timestamp completedAt;
    @Column
    private Timestamp arrivalNotificationSentAt;

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

    public DeliveryRoute getDeliveryRoute() {
        return deliveryRoute;
    }

    public void setDeliveryRoute(DeliveryRoute deliveryRoute) {
        this.deliveryRoute = deliveryRoute;
    }

    public Package getaPackage() {
        return aPackage;
    }

    public void setaPackage(Package aPackage) {
        this.aPackage = aPackage;
    }

    public DELIVERY_ROUTE_STOP_KIND getKind() {
        return kind;
    }

    public void setKind(DELIVERY_ROUTE_STOP_KIND kind) {
        this.kind = kind;
    }

    public DELIVERY_ROUTE_STOP_STATUS getStatus() {
        return status;
    }

    public void setStatus(DELIVERY_ROUTE_STOP_STATUS status) {
        this.status = status;
    }

    public Integer getStopOrder() {
        return stopOrder;
    }

    public void setStopOrder(Integer stopOrder) {
        this.stopOrder = stopOrder;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getAddressLabel() {
        return addressLabel;
    }

    public void setAddressLabel(String addressLabel) {
        this.addressLabel = addressLabel;
    }

    public Double getPlannedWeightKg() {
        return plannedWeightKg;
    }

    public void setPlannedWeightKg(Double plannedWeightKg) {
        this.plannedWeightKg = plannedWeightKg;
    }

    public Double getPlannedVolumeCm3() {
        return plannedVolumeCm3;
    }

    public void setPlannedVolumeCm3(Double plannedVolumeCm3) {
        this.plannedVolumeCm3 = plannedVolumeCm3;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

    public Timestamp getArrivalNotificationSentAt() {
        return arrivalNotificationSentAt;
    }

    public void setArrivalNotificationSentAt(Timestamp arrivalNotificationSentAt) {
        this.arrivalNotificationSentAt = arrivalNotificationSentAt;
    }
}
