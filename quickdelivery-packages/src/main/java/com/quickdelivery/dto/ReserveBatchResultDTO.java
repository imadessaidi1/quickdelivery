package com.quickdelivery.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReserveBatchResultDTO {
    private Long deliveryPersonId;
    private int requestedCount;
    private int reservedCount;
    private List<Long> reservedPackageIds = new ArrayList<>();
    private Map<Long, String> skippedPackages = new LinkedHashMap<>();

    public Long getDeliveryPersonId() {
        return deliveryPersonId;
    }

    public void setDeliveryPersonId(Long deliveryPersonId) {
        this.deliveryPersonId = deliveryPersonId;
    }

    public int getRequestedCount() {
        return requestedCount;
    }

    public void setRequestedCount(int requestedCount) {
        this.requestedCount = requestedCount;
    }

    public int getReservedCount() {
        return reservedCount;
    }

    public void setReservedCount(int reservedCount) {
        this.reservedCount = reservedCount;
    }

    public List<Long> getReservedPackageIds() {
        return reservedPackageIds;
    }

    public void setReservedPackageIds(List<Long> reservedPackageIds) {
        this.reservedPackageIds = reservedPackageIds;
    }

    public Map<Long, String> getSkippedPackages() {
        return skippedPackages;
    }

    public void setSkippedPackages(Map<Long, String> skippedPackages) {
        this.skippedPackages = skippedPackages;
    }
}
