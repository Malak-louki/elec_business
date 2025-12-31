package com.hb.cda.elec_business.entity;

public enum UserStatus {
    PENDING,   // Inscription reçue, doit cliquer sur le lien d’activation
    ACTIVE,    // Compte activé et fonctionnel
    SUSPENDED, // Suspension temporaire décidée par un admin
    BANNED
}
