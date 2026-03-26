package com.quickdelivery.abstarct.dto;

public class AdminUserOverviewDTO {
    private long registeredCustomers;
    private long registeredDeliveryPersons;
    private long connectedCustomers;
    private long connectedDeliveryPersons;

    public long getRegisteredCustomers() {
        return registeredCustomers;
    }

    public void setRegisteredCustomers(long registeredCustomers) {
        this.registeredCustomers = registeredCustomers;
    }

    public long getRegisteredDeliveryPersons() {
        return registeredDeliveryPersons;
    }

    public void setRegisteredDeliveryPersons(long registeredDeliveryPersons) {
        this.registeredDeliveryPersons = registeredDeliveryPersons;
    }

    public long getConnectedCustomers() {
        return connectedCustomers;
    }

    public void setConnectedCustomers(long connectedCustomers) {
        this.connectedCustomers = connectedCustomers;
    }

    public long getConnectedDeliveryPersons() {
        return connectedDeliveryPersons;
    }

    public void setConnectedDeliveryPersons(long connectedDeliveryPersons) {
        this.connectedDeliveryPersons = connectedDeliveryPersons;
    }
}
