package com.quickdelivery.abstarct.dto;

import com.quickdelivery.abstarct.parameters.DEVICE_PLATFORM;
import java.math.BigDecimal;

public class MobileDeviceRegistrationDTO {
    private Long userId;
    private String deviceId;
    private String pushToken;
    private String locale;
    private DEVICE_PLATFORM platform;
    private Boolean active;
    private BigDecimal latitude;
    private BigDecimal longitude;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }
}
