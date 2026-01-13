package com.hb.cda.elec_business.repository;

import com.hb.cda.elec_business.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    /**
     * Trouve une réservation avec toutes ses relations
     */
    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.user " +
            "LEFT JOIN FETCH b.payment " +
            "LEFT JOIN FETCH b.chargingStation cs " +
            "LEFT JOIN FETCH cs.chargingLocation cl " +
            "LEFT JOIN FETCH cl.address " +
            "WHERE b.id = :id")
    Optional<Booking> findByIdWithRelations(@Param("id") String id);

    /**
     * Trouve toutes les réservations d'un utilisateur
     */
    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.chargingStation cs " +
            "LEFT JOIN FETCH cs.chargingLocation cl " +
            "LEFT JOIN FETCH cl.address " +
            "LEFT JOIN FETCH b.payment " +
            "WHERE b.user = :user " +
            "ORDER BY b.startDateTime DESC")
    List<Booking> findByUserWithRelations(@Param("user") User user);

    /**
     * Trouve toutes les réservations pour une station
     */
    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.user " +
            "LEFT JOIN FETCH b.payment " +
            "WHERE b.chargingStation = :station " +
            "ORDER BY b.startDateTime DESC")
    List<Booking> findByChargingStationWithRelations(@Param("station") ChargingStation station);

    /**
     * Trouve les réservations par statut
     */
    List<Booking> findByUserAndBookingStatusOrderByStartDateTimeDesc(User user, BookingStatus status);

    /**
     * Vérifie s'il existe un conflit de réservation
     * On vérifie uniquement PENDING et CONFIRMED
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b " +
            "WHERE b.chargingStation = :station " +
            "AND b.bookingStatus IN ('CONFIRMED', 'PENDING') " +
            "AND b.startDateTime < :endDateTime " +
            "AND b.endDateTime > :startDateTime")
    boolean existsConflictingBooking(
            @Param("station") ChargingStation station,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    /**
     * Trouve les réservations à venir pour un utilisateur
     */
    @Query("SELECT b FROM Booking b " +
            "WHERE b.user = :user " +
            "AND b.startDateTime > :now " +
            "AND b.bookingStatus IN ('CONFIRMED', 'PENDING') " +
            "ORDER BY b.startDateTime")
    List<Booking> findUpcomingBookingsByUser(
            @Param("user") User user,
            @Param("now") LocalDateTime now
    );

    /**
     * Trouve les réservations passées pour un utilisateur
     */
    @Query("SELECT b FROM Booking b " +
            "WHERE b.user = :user " +
            "AND b.endDateTime < :now " +
            "ORDER BY b.endDateTime DESC")
    List<Booking> findPastBookingsByUser(
            @Param("user") User user,
            @Param("now") LocalDateTime now
    );

    /**
     * Trouve les réservations PENDING qui ont expiré
     */
    @Query("SELECT b FROM Booking b " +
            "WHERE b.bookingStatus = 'PENDING' " +
            "AND b.expiresAt < :now")
    List<Booking> findExpiredPendingBookings(@Param("now") Instant now);

    /**
     * Trouve les réservations CONFIRMED à marquer comme COMPLETED
     */
    @Query("SELECT b FROM Booking b " +
            "WHERE b.bookingStatus = 'CONFIRMED' " +
            "AND b.endDateTime < :cutoffTime")
    List<Booking> findBookingsToComplete(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Compte les réservations ACTIVES uniquement
     * PENDING + CONFIRMED (pas les CANCELLED, EXPIRED, COMPLETED)
     */
    @Query("SELECT COUNT(b) FROM Booking b " +
            "WHERE b.user = :user " +
            "AND b.bookingStatus IN ('PENDING', 'CONFIRMED')")
    long countActiveBookingsByUser(@Param("user") User user);

    /**
     * Compte les réservations créées aujourd'hui
     */
    @Query("SELECT COUNT(b) FROM Booking b " +
            "WHERE b.user = :user " +
            "AND b.createdAt >= :todayStart")
    long countBookingsCreatedTodayByUser(
            @Param("user") User user,
            @Param("todayStart") Instant todayStart
    );
    /**
     * Trouve une réservation par son paiement
     */
    Optional<Booking> findByPayment(Payment payment);
}
