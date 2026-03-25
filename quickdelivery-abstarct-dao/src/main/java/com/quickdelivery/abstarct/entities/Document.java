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
    private String ocrDocumentType;
    @Column
    private String ocrDetectedDocumentType;
    @Column
    private Boolean ocrTypeConsistent;
    @Column
    private String ocrLastName;
    @Column
    private String ocrFirstName;
    @Column
    private String ocrBirthDate;
    @Column
    private String ocrExpiryDate;
    @Column
    private String ocrRegistrationNumber;
    @Column
    private String ocrBrand;
    @Column
    private String ocrModel;
    @Column
    private String ocrEnergyType;
    @Column
    private String ocrHolderName;
    @Column
    private String ocrCompanyName;
    @Column
    private String ocrSiren;
    @Column
    private String ocrInsuranceKind;
    @Column
    private String ocrIban;
    @Column
    private String ocrBic;
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

    public String getOcrDocumentType() {
        return ocrDocumentType;
    }

    public void setOcrDocumentType(String ocrDocumentType) {
        this.ocrDocumentType = ocrDocumentType;
    }

    public String getOcrDetectedDocumentType() {
        return ocrDetectedDocumentType;
    }

    public void setOcrDetectedDocumentType(String ocrDetectedDocumentType) {
        this.ocrDetectedDocumentType = ocrDetectedDocumentType;
    }

    public Boolean getOcrTypeConsistent() {
        return ocrTypeConsistent;
    }

    public void setOcrTypeConsistent(Boolean ocrTypeConsistent) {
        this.ocrTypeConsistent = ocrTypeConsistent;
    }

    public String getOcrLastName() {
        return ocrLastName;
    }

    public void setOcrLastName(String ocrLastName) {
        this.ocrLastName = ocrLastName;
    }

    public String getOcrFirstName() {
        return ocrFirstName;
    }

    public void setOcrFirstName(String ocrFirstName) {
        this.ocrFirstName = ocrFirstName;
    }

    public String getOcrBirthDate() {
        return ocrBirthDate;
    }

    public void setOcrBirthDate(String ocrBirthDate) {
        this.ocrBirthDate = ocrBirthDate;
    }

    public String getOcrExpiryDate() {
        return ocrExpiryDate;
    }

    public void setOcrExpiryDate(String ocrExpiryDate) {
        this.ocrExpiryDate = ocrExpiryDate;
    }

    public String getOcrRegistrationNumber() {
        return ocrRegistrationNumber;
    }

    public void setOcrRegistrationNumber(String ocrRegistrationNumber) {
        this.ocrRegistrationNumber = ocrRegistrationNumber;
    }

    public String getOcrBrand() {
        return ocrBrand;
    }

    public void setOcrBrand(String ocrBrand) {
        this.ocrBrand = ocrBrand;
    }

    public String getOcrModel() {
        return ocrModel;
    }

    public void setOcrModel(String ocrModel) {
        this.ocrModel = ocrModel;
    }

    public String getOcrEnergyType() {
        return ocrEnergyType;
    }

    public void setOcrEnergyType(String ocrEnergyType) {
        this.ocrEnergyType = ocrEnergyType;
    }

    public String getOcrHolderName() {
        return ocrHolderName;
    }

    public void setOcrHolderName(String ocrHolderName) {
        this.ocrHolderName = ocrHolderName;
    }

    public String getOcrCompanyName() {
        return ocrCompanyName;
    }

    public void setOcrCompanyName(String ocrCompanyName) {
        this.ocrCompanyName = ocrCompanyName;
    }

    public String getOcrSiren() {
        return ocrSiren;
    }

    public void setOcrSiren(String ocrSiren) {
        this.ocrSiren = ocrSiren;
    }

    public String getOcrInsuranceKind() {
        return ocrInsuranceKind;
    }

    public void setOcrInsuranceKind(String ocrInsuranceKind) {
        this.ocrInsuranceKind = ocrInsuranceKind;
    }

    public String getOcrIban() {
        return ocrIban;
    }

    public void setOcrIban(String ocrIban) {
        this.ocrIban = ocrIban;
    }

    public String getOcrBic() {
        return ocrBic;
    }

    public void setOcrBic(String ocrBic) {
        this.ocrBic = ocrBic;
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
