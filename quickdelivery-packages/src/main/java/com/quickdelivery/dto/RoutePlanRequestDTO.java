package com.quickdelivery.dto;

import java.util.ArrayList;
import java.util.List;

public class RoutePlanRequestDTO {
    private String mode;
    private String deliveryMode;
    private String vehicleType;
    private Long selectedPackageId;
    private RoutePlanPointDTO start;
    private RoutePlanPointDTO end;
    private List<Long> packageIds = new ArrayList<>();

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Long getSelectedPackageId() {
        return selectedPackageId;
    }

    public void setSelectedPackageId(Long selectedPackageId) {
        this.selectedPackageId = selectedPackageId;
    }

    public RoutePlanPointDTO getStart() {
        return start;
    }

    public void setStart(RoutePlanPointDTO start) {
        this.start = start;
    }

    public RoutePlanPointDTO getEnd() {
        return end;
    }

    public void setEnd(RoutePlanPointDTO end) {
        this.end = end;
    }

    public List<Long> getPackageIds() {
        return packageIds;
    }

    public void setPackageIds(List<Long> packageIds) {
        this.packageIds = packageIds;
    }
}
