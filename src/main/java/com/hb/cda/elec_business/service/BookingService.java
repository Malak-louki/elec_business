package com.hb.cda.elec_business.service;

import com.hb.cda.elec_business.dto.booking.BookingRequestDto;
import com.hb.cda.elec_business.dto.booking.BookingResponseDto;
import com.hb.cda.elec_business.entity.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface pour le service de gestion des réservations
 *
 * @author Votre Nom - CDA 2026
 */
public interface BookingService {

    /**
     * Crée une nouvelle réservation
     *
     * @param request Données de la réservation (borne, dates)
     * @param user Utilisateur qui fait la réservation
     * @return Réservation créée avec statut PENDING
     * @throws com.hb.cda.elec_business.exception.BookingConflictException si créneau déjà pris
     * @throws com.hb.cda.elec_business.exception.BookingValidationException si données invalides
     */
    BookingResponseDto createBooking(BookingRequestDto request, User user);

    /**
     * Annule une réservation
     *
     * @param bookingId ID de la réservation
     * @param user Utilisateur qui annule (doit être le propriétaire)
     * @return Réservation avec statut CANCELLED
     * @throws com.hb.cda.elec_business.exception.BookingValidationException si annulation impossible
     * @throws org.springframework.security.access.AccessDeniedException si pas le propriétaire
     */
    BookingResponseDto cancelBooking(String bookingId, User user);

    /**
     * Confirme une réservation après paiement
     *
     * @param bookingId ID de la réservation
     * @param paymentId ID du paiement validé
     * @return Réservation avec statut CONFIRMED
     * @throws com.hb.cda.elec_business.exception.BookingValidationException si confirmation impossible
     */
    BookingResponseDto confirmBooking(String bookingId, String paymentId);

    /**
     * Marque une réservation comme terminée
     *
     * @param bookingId ID de la réservation
     * @return Réservation avec statut COMPLETED
     * @throws com.hb.cda.elec_business.exception.BookingValidationException si finalisation impossible
     */
    BookingResponseDto completeBooking(String bookingId);

    /**
     * Récupère les détails d'une réservation
     *
     * @param bookingId ID de la réservation
     * @param user Utilisateur connecté (propriétaire réservation ou propriétaire borne)
     * @return Détails de la réservation
     * @throws com.hb.cda.elec_business.exception.BookingValidationException si réservation non trouvée
     * @throws org.springframework.security.access.AccessDeniedException si pas autorisé
     */
    BookingResponseDto getBookingById(String bookingId, User user);

    /**
     * Récupère toutes les réservations d'un utilisateur
     *
     * @param user Utilisateur connecté
     * @return Liste de toutes ses réservations (tous statuts)
     */
    List<BookingResponseDto> getMyBookings(User user);

    /**
     * Récupère les réservations d'un utilisateur par statut
     *
     * @param user Utilisateur connecté
     * @param status Statut recherché (PENDING, CONFIRMED, etc.)
     * @return Liste des réservations avec ce statut
     * @throws com.hb.cda.elec_business.exception.BookingValidationException si statut invalide
     */
    List<BookingResponseDto> getMyBookingsByStatus(User user, String status);

    /**
     * Récupère les réservations à venir d'un utilisateur
     *
     * @param user Utilisateur connecté
     * @return Réservations PENDING ou CONFIRMED dont startDateTime > maintenant
     */
    List<BookingResponseDto> getMyUpcomingBookings(User user);

    /**
     * Récupère les réservations passées d'un utilisateur
     *
     * @param user Utilisateur connecté
     * @return Réservations dont endDateTime < maintenant
     */
    List<BookingResponseDto> getMyPastBookings(User user);

    /**
     * Récupère toutes les réservations d'une borne
     *
     * @param stationId ID de la borne
     * @param owner Propriétaire de la borne
     * @return Liste des réservations de cette borne
     * @throws com.hb.cda.elec_business.exception.BookingValidationException si borne non trouvée
     * @throws org.springframework.security.access.AccessDeniedException si pas le propriétaire
     */
    List<BookingResponseDto> getBookingsByStation(String stationId, User owner);

    /**
     * Vérifie si une borne est disponible sur un créneau
     *
     * @param stationId ID de la borne
     * @param startDateTime Date/heure de début
     * @param endDateTime Date/heure de fin
     * @return true si disponible, false sinon
     * @throws com.hb.cda.elec_business.exception.BookingValidationException si borne non trouvée
     */
    boolean checkAvailability(String stationId, LocalDateTime startDateTime, LocalDateTime endDateTime);
}