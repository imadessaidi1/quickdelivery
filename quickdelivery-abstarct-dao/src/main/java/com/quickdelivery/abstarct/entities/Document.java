package com.quickdelivery.abstarct.entities;

import com.quickdelivery.abstarct.parameters.DOCUMENT_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;
import com.quickdelivery.abstarct.parameters.DOCUMENT_MATCH_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_OCR_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_VALIDATION_STATUS;
import jakarta.persistence.*;

import java.util.Date;

@Entity
public class Document {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    @Version
    private Integer version;
    @Column
    private String docURL;
    @Column
    @Enumerated(EnumType.STRING)
    private DOCUMENT_TYPE type;
    @Column
    private DOCUMENT_STATUS documentStatus;
    @Column(length = 1000)
    private String reviewComment;
    @Column
    private Date reviewedAt;
    @Column
    private String reviewedBy;
    @Column
    @Enumerated(EnumType.STRING)
    private DOCUMENT_VALIDATION_STATUS validationStatus;
    @Column
    private String validationCode;
    @Column(length = 1000)
    private String validationDetails;
    @Column
    private Boolean validatedAutomatically;
    @Column
    @Enumerated(EnumType.STRING)
    private DOCUMENT_OCR_STATUS ocrStatus;
    @Column
    private String ocrProvider;
    @Column
    private Double ocrConfidenceScore;
    @Column(length = 8000)
    private String ocrExtractedData;
    @Column
    private Date ocrProcessedAt;
    @Column
    private String ocrErrorCode;
    @Column
    @Enumerated(EnumType.STRING)
    private DOCUMENT_MATCH_STATUS matchStatus;
    @Column
    private Double matchScore;
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String matchDetails;
    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name="vehicle_id")
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name="package_id")
    private Package aPackage;

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

    public Package getaPackage() {
        return aPackage;
    }

    public void setaPackage(Package aPackage) {
        this.aPackage = aPackage;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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
