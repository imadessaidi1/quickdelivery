package com.quickdelivery.dto;

public class ReservationAvailabilityDTO {
    private boolean canReserve;
    private boolean activeRouteBlocking;
    private boolean capacityReached;
    private int activeReservations;
    private int maxReservations;
    private String reason;
    private Double financialPenaltyAmount;
    private String financialPenaltyCurrency;
    private String suspensionUntil;

    public boolean isCanReserve() {
        return canReserve;
    }

    public void setCanReserve(boolean canReserve) {
        this.canReserve = canReserve;
    }

    public boolean isActiveRouteBlocking() {
        return activeRouteBlocking;
    }

    public void setActiveRouteBlocking(boolean activeRouteBlocking) {
        this.activeRouteBlocking = activeRouteBlocking;
    }

    public boolean isCapacityReached() {
        return capacityReached;
    }

    public void setCapacityReached(boolean capacityReached) {
        this.capacityReached = capacityReached;
    }

    public int getActiveReservations() {
        return activeReservations;
    }

    public void setActiveReservations(int activeReservations) {
        this.activeReservations = activeReservations;
    }

    public int getMaxReservations() {
        return maxReservations;
    }

    public void setMaxReservations(int maxReservations) {
        this.maxReservations = maxReservations;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Double getFinancialPenaltyAmount() {
        return financialPenaltyAmount;
    }

    public void setFinancialPenaltyAmount(Double financialPenaltyAmount) {
        this.financialPenaltyAmount = financialPenaltyAmount;
    }

    public String getFinancialPenaltyCurrency() {
        return financialPenaltyCurrency;
    }

    public void setFinancialPenaltyCurrency(String financialPenaltyCurrency) {
        this.financialPenaltyCurrency = financialPenaltyCurrency;
    }

    public String getSuspensionUntil() {
        return suspensionUntil;
    }

    public void setSuspensionUntil(String suspensionUntil) {
        this.suspensionUntil = suspensionUntil;
    }
}
