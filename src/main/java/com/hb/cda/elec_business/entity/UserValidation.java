package com.hb.cda.elec_business.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class UserValidation  extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String confirmationCode;
    private Instant validatedAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public UserValidation() {
    }

    public UserValidation( String confirmationCode, Instant validatedAt) {
        this.confirmationCode = confirmationCode;
        this.validatedAt = validatedAt;
    }

    public UserValidation(String id, String confirmationCode, Instant validatedAt) {
        this.id = id;
        this.confirmationCode = confirmationCode;
        this.validatedAt = validatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    public Instant getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
