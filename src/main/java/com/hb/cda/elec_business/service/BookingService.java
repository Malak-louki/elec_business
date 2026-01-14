package com.hb.cda.elec_business.service;

import com.hb.cda.elec_business.dto.booking.BookingRequestDto;
import com.hb.cda.elec_business.dto.booking.BookingResponseDto;
import com.hb.cda.elec_business.entity.BookingStatus;
import com.hb.cda.elec_business.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service pour la gestion des réservations de bornes de recharge
 */
public interface BookingService {

    // ====================================================================
    // GESTION DES RÉSERVATIONS (CLIENTS)
    // ====================================================================

    /**
     * Créer une nouvelle réservation
     * Vérifie la disponibilité et calcule automatiquement le montant
     */
    BookingResponseDto createBooking(BookingRequestDto request, User customer);

    /**
     * Récupérer une réservation par ID
     */
    BookingResponseDto getBookingById(String bookingId, User user);

    /**
     * Récupérer toutes les réservations d'un utilisateur
     */
    List<BookingResponseDto> getMyBookings(User customer);

    /**
     * Récupérer les réservations d'un utilisateur par statut
     */
    List<BookingResponseDto> getMyBookingsByStatus(User customer, BookingStatus status);

    /**
     * Annuler une réservation
     * Seul le client ou le propriétaire de la borne peut annuler
     */
    void cancelBooking(String bookingId, User user);

    /**
     * Mettre à jour le statut d'une réservation
     * (utilisé après confirmation de paiement)
     */
    BookingResponseDto updateBookingStatus(String bookingId, BookingStatus newStatus);

    // ====================================================================
    // VÉRIFICATION DE DISPONIBILITÉ
    // ====================================================================

    /**
     * Vérifie si un créneau est disponible pour une borne
     * @return true si disponible, false sinon
     */
    boolean isSlotAvailable(
            String stationId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    /**
     * Récupère les créneaux déjà réservés pour une borne sur une période
     */
    List<BookingResponseDto> getBookedSlotsForStation(
            String stationId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    // ====================================================================
    // MÉTHODES POUR LE DASHBOARD OWNER
    // ====================================================================

    /**
     * Récupère toutes les réservations pour les bornes d'un propriétaire
     */
    List<BookingResponseDto> getAllBookingsForOwner(User owner);

    /**
     * Récupère les réservations d'une borne spécifique
     * Vérifie que l'utilisateur est bien le propriétaire
     */
    List<BookingResponseDto> getBookingsForStation(String stationId, User owner);

    /**
     * Récupère les réservations confirmées pour une borne
     */
    List<BookingResponseDto> getConfirmedBookingsForStation(String stationId, User owner);

    /**
     * Récupère les réservations à venir pour un propriétaire
     */
    List<BookingResponseDto> getUpcomingBookingsForOwner(User owner);

    /**
     * Récupère les réservations passées pour un propriétaire
     */
    List<BookingResponseDto> getPastBookingsForOwner(User owner);

    // ====================================================================
    // STATISTIQUES ET ANALYTICS (POUR DASHBOARD OWNER)
    // ====================================================================

    /**
     * Calcule le revenu total généré par toutes les bornes d'un propriétaire
     */
    BigDecimal calculateTotalRevenueForOwner(User owner);

    /**
     * Calcule le revenu total pour une borne spécifique
     */
    BigDecimal calculateRevenueForStation(String stationId, User owner);

    /**
     * Calcule le revenu sur une période donnée
     */
    BigDecimal calculateRevenueBetweenDates(
            User owner,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * Compte le nombre total de réservations pour un propriétaire
     */
    Long countTotalBookingsForOwner(User owner);

    /**
     * Compte le nombre de réservations pour une borne
     */
    Long countBookingsForStation(String stationId, User owner);

    /**
     * Compte le nombre de clients uniques ayant réservé chez un propriétaire
     */
    Long countUniqueCustomersForOwner(User owner);

    /**
     * Calcule le taux d'occupation d'une borne sur une période
     * @return Pourcentage entre 0 et 100
     */
    Double calculateOccupancyRate(String stationId, LocalDate startDate, LocalDate endDate);
}