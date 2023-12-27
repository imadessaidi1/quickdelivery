package com.quickdelivery.abstarct.entities;

import com.quickdelivery.abstarct.parameters.DOCUMENT_TYPE;
import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
public class Document {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    @Version
    private Timestamp version;
    @Column
    private String docURL;
    @Column
    @Enumerated(EnumType.STRING)
    private DOCUMENT_TYPE type;
    @Lob
    @Column(name = "doc_content", columnDefinition = "LONGBLOB")
    private byte[] docContent;
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

    public Timestamp getVersion() {
        return version;
    }

    public void setVersion(Timestamp version) {
        this.version = version;
    }

    public byte[] getDocContent() {
        return docContent;
    }

    public void setDocContent(byte[] docContent) {
        this.docContent = docContent;
    }
}
