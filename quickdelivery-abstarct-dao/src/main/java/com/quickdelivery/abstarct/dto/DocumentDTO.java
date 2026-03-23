package com.quickdelivery.abstarct.dto;


import com.quickdelivery.abstarct.parameters.DOCUMENT_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;

import java.util.Date;

public class DocumentDTO {
    private Long id;
    private Integer version;
    private String docURL;
    private DOCUMENT_TYPE type;
    private String fileName;
    private byte[] data;

    private DOCUMENT_STATUS documentStatus;
    private String reviewComment;
    private Date reviewedAt;
    private String reviewedBy;

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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
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

    public DOCUMENT_STATUS getDocumentStatus() {
        return documentStatus;
    }

    public void setDocumentStatus(DOCUMENT_STATUS documentStatus) {
        this.documentStatus = documentStatus;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public Date getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Date reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
}
