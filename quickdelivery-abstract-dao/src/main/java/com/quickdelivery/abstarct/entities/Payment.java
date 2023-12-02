package com.quickdelivery.abstarct.entities;

import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
public class Payment {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    @Version
    private Timestamp version;
    @Column
    protected Boolean validated;
    @ManyToOne
    @JoinColumn(name="holderInApp_id", nullable=false)
    private User holderInApp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getValidated() {
        return validated;
    }

    public void setValidated(Boolean validated) {
        this.validated = validated;
    }

    public User getHolderInApp() {
        return holderInApp;
    }

    public void setHolderInApp(User holderInApp) {
        this.holderInApp = holderInApp;
    }

    public Timestamp getVersion() {
        return version;
    }

    public void setVersion(Timestamp version) {
        this.version = version;
    }
}
