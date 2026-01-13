package com.hb.cda.elec_business.exception;

/**
 * Exception levée quand une validation métier échoue
 *
 * Exemples :
 * - Durée trop courte/longue
 * - Date dans le passé
 * - Annulation hors délai
 */
public class BookingValidationException extends RuntimeException {

    public BookingValidationException(String message) {
        super(message);
    }

    public BookingValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}