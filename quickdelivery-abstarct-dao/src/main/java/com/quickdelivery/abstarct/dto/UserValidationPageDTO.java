package com.quickdelivery.abstarct.dto;

import java.util.ArrayList;
import java.util.List;

public class UserValidationPageDTO {
    private List<UserDTO> items = new ArrayList<>();
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
    private long totalVehicles;
    private long totalDocuments;
    private long totalVehicleDocuments;

    public List<UserDTO> getItems() {
        return items;
    }

    public void setItems(List<UserDTO> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(long totalItems) {
        this.totalItems = totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalVehicles() {
        return totalVehicles;
    }

    public void setTotalVehicles(long totalVehicles) {
        this.totalVehicles = totalVehicles;
    }

    public long getTotalDocuments() {
        return totalDocuments;
    }

    public void setTotalDocuments(long totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    public long getTotalVehicleDocuments() {
        return totalVehicleDocuments;
    }

    public void setTotalVehicleDocuments(long totalVehicleDocuments) {
        this.totalVehicleDocuments = totalVehicleDocuments;
    }
}
