package com.quickdelivery.abstarct.entities;

import com.quickdelivery.abstarct.parameters.DEVICE_PLATFORM;
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
import java.math.BigDecimal;

@Entity
public class MobileDevice {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Version
    private Integer version;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(nullable = false, length = 120)
    private String deviceId;
    @Column(length = 1024)
    private String pushToken;
    @Column(length = 10)
    private String locale;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DEVICE_PLATFORM platform;
    @Column(nullable = false)
    private Boolean active;
    @Column(nullable = false)
    private Timestamp createdAt;
    @Column(nullable = false)
    private Timestamp updatedAt;
    @Column
    private Timestamp lastSeenAt;
    @Column(precision = 10, scale = 7)
    private BigDecimal lastLatitude;
    @Column(precision = 10, scale = 7)
    private BigDecimal lastLongitude;
    @Column
    private Timestamp lastLocationAt;

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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getPushToken() {
        return pushToken;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public DEVICE_PLATFORM getPlatform() {
        return platform;
    }

    public void setPlatform(DEVICE_PLATFORM platform) {
        this.platform = platform;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Timestamp getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Timestamp lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public BigDecimal getLastLatitude() {
        return lastLatitude;
    }

    public void setLastLatitude(BigDecimal lastLatitude) {
        this.lastLatitude = lastLatitude;
    }

    public BigDecimal getLastLongitude() {
        return lastLongitude;
    }

    public void setLastLongitude(BigDecimal lastLongitude) {
        this.lastLongitude = lastLongitude;
    }

    public Timestamp getLastLocationAt() {
        return lastLocationAt;
    }

    public void setLastLocationAt(Timestamp lastLocationAt) {
        this.lastLocationAt = lastLocationAt;
    }
}
