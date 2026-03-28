package com.quickdelivery.abstarct.dto;

public class FinancialTrendPointDTO {
    private String periodKey;
    private Double value;

    public String getPeriodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
