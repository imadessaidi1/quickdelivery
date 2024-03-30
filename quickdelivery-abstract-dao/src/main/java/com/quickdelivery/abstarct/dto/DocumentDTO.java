package com.quickdelivery.abstarct.dto;


import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;

import java.sql.Timestamp;

public class DocumentDTO {
    private Long id;
    private Timestamp version;
    private String docURL;
    private DOCUMENT_TYPE type;
    private String fileName;
    private byte[] data;

    public DocumentDTO(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDocURL() {
        return docURL;
    }

    public void setDocURL(String docURL) {
        this.docURL = docURL;
    }

    public DOCUMENT_TYPE getType() {
        return type;
    }

    public void setType(DOCUMENT_TYPE type) {
        this.type = type;
    }

    public Timestamp getVersion() {
        return version;
    }

    public void setVersion(Timestamp version) {
        this.version = version;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
