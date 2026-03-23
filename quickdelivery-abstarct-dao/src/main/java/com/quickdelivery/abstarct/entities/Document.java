package com.quickdelivery.abstarct.entities;

import com.quickdelivery.abstarct.parameters.DOCUMENT_STATUS;
import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;
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
}
