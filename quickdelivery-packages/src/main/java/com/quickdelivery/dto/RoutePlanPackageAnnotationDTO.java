package com.quickdelivery.dto;

public class RoutePlanPackageAnnotationDTO {
    private Integer sortOrder;
    private Integer pickupOrder;
    private Integer dropoffOrder;
    private Integer stopCount;

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getPickupOrder() {
        return pickupOrder;
    }

    public void setPickupOrder(Integer pickupOrder) {
        this.pickupOrder = pickupOrder;
    }

    public Integer getDropoffOrder() {
        return dropoffOrder;
    }

    public void setDropoffOrder(Integer dropoffOrder) {
        this.dropoffOrder = dropoffOrder;
    }

    public Integer getStopCount() {
        return stopCount;
    }

    public void setStopCount(Integer stopCount) {
        this.stopCount = stopCount;
    }
}
