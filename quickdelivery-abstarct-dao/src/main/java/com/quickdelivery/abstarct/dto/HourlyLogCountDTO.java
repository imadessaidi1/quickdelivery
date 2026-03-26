package com.quickdelivery.abstarct.dto;

public class HourlyLogCountDTO {
    private String hourLabel;
    private Long warnCount;
    private Long errorCount;

    public String getHourLabel() {
        return hourLabel;
    }

    public void setHourLabel(String hourLabel) {
        this.hourLabel = hourLabel;
    }

    public Long getWarnCount() {
        return warnCount;
    }

    public void setWarnCount(Long warnCount) {
        this.warnCount = warnCount;
    }

    public Long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Long errorCount) {
        this.errorCount = errorCount;
    }
}
