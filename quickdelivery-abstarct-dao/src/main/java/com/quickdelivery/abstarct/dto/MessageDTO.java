package com.quickdelivery.abstarct.dto;

public class MessageDTO {
    private String type;
    private String from;
    private String to;
    private String message;
    private String url;

    private PositionDTO positionDTO;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public PositionDTO getPositionDTO() {
        return positionDTO;
    }

    public void setPositionDTO(PositionDTO positionDTO) {
        this.positionDTO = positionDTO;
    }
}
