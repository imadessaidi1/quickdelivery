package com.quickdelivery.abstarct.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.quickdelivery.abstarct.parameters.DOCUMENT_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;
import com.quickdelivery.abstarct.parameters.DOCUMENT_MATCH_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_OCR_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_VALIDATION_STATUS;

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
    private DOCUMENT_VALIDATION_STATUS validationStatus;
    private String validationCode;
    private String validationDetails;
    private Boolean validatedAutomatically;
    private DOCUMENT_OCR_STATUS ocrStatus;
    private String ocrProvider;
    private Double ocrConfidenceScore;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String ocrExtractedData;
    private Date ocrProcessedAt;
    private String ocrErrorCode;
    private DOCUMENT_MATCH_STATUS matchStatus;
    private Double matchScore;
    private String matchDetails;

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

    public DOCUMENT_VALIDATION_STATUS getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(DOCUMENT_VALIDATION_STATUS validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getValidationCode() {
        return validationCode;
    }

    public void setValidationCode(String validationCode) {
        this.validationCode = validationCode;
    }

    public String getValidationDetails() {
        return validationDetails;
    }

    public void setValidationDetails(String validationDetails) {
        this.validationDetails = validationDetails;
    }

    public Boolean getValidatedAutomatically() {
        return validatedAutomatically;
    }

    public void setValidatedAutomatically(Boolean validatedAutomatically) {
        this.validatedAutomatically = validatedAutomatically;
    }

    public DOCUMENT_OCR_STATUS getOcrStatus() {
        return ocrStatus;
    }

    public void setOcrStatus(DOCUMENT_OCR_STATUS ocrStatus) {
        this.ocrStatus = ocrStatus;
    }

    public String getOcrProvider() {
        return ocrProvider;
    }

    public void setOcrProvider(String ocrProvider) {
        this.ocrProvider = ocrProvider;
    }

    public Double getOcrConfidenceScore() {
        return ocrConfidenceScore;
    }

    public void setOcrConfidenceScore(Double ocrConfidenceScore) {
        this.ocrConfidenceScore = ocrConfidenceScore;
    }

    public String getOcrExtractedData() {
        return ocrExtractedData;
    }

    public void setOcrExtractedData(String ocrExtractedData) {
        this.ocrExtractedData = ocrExtractedData;
    }

    public Date getOcrProcessedAt() {
        return ocrProcessedAt;
    }

    public void setOcrProcessedAt(Date ocrProcessedAt) {
        this.ocrProcessedAt = ocrProcessedAt;
    }

    public String getOcrErrorCode() {
        return ocrErrorCode;
    }

    public void setOcrErrorCode(String ocrErrorCode) {
        this.ocrErrorCode = ocrErrorCode;
    }

    public DOCUMENT_MATCH_STATUS getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(DOCUMENT_MATCH_STATUS matchStatus) {
        this.matchStatus = matchStatus;
    }

    public Double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Double matchScore) {
        this.matchScore = matchScore;
    }

    public String getMatchDetails() {
        return matchDetails;
    }

    public void setMatchDetails(String matchDetails) {
        this.matchDetails = matchDetails;
    }
}
