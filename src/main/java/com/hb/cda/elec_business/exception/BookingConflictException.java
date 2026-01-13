package com.hb.cda.elec_business.exception;

/**
 * Exception levée quand une réservation entre en conflit avec une autre
 *
 * Exemple : Utilisateur A et B réservent le même créneau simultanément
 */
public class BookingConflictException extends RuntimeException {

    public BookingConflictException(String message) {
        super(message);
    }

    public BookingConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}