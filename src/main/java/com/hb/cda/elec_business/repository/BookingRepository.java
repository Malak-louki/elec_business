package com.hb.cda.elec_business.repository;

import com.hb.cda.elec_business.entity.Booking;
import com.hb.cda.elec_business.entity.BookingStatus;
import com.hb.cda.elec_business.entity.ChargingStation;
import com.hb.cda.elec_business.entity.Payment;
import com.hb.cda.elec_business.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    // ====================================================================
    // FETCH RELATIONS
    // ====================================================================

    /**
     * Trouve une réservation par ID avec toutes ses relations.
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.user
        LEFT JOIN FETCH b.payment
        LEFT JOIN FETCH b.chargingStation cs
        LEFT JOIN FETCH cs.chargingLocation cl
        LEFT JOIN FETCH cl.address
        WHERE b.id = :id
    """)
    Optional<Booking> findByIdWithRelations(@Param("id") String id);

    /**
     * Trouve toutes les réservations d'un utilisateur avec relations.
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.chargingStation cs
        LEFT JOIN FETCH cs.chargingLocation cl
        LEFT JOIN FETCH cl.address
        LEFT JOIN FETCH b.payment
        WHERE b.user = :user
        ORDER BY b.startDateTime DESC
    """)
    List<Booking> findByUserWithRelations(@Param("user") User user);

    /**
     * Trouve toutes les réservations pour une station avec relations.
     */
    @Query("""
        SELECT DISTINCT b FROM Booking b
        LEFT JOIN FETCH b.user
        LEFT JOIN FETCH b.payment
        WHERE b.chargingStation = :station
        ORDER BY b.startDateTime DESC
    """)
    List<Booking> findByChargingStationWithRelations(@Param("station") ChargingStation station);

    // ====================================================================
    // REQUÊTES UTILISATEUR (CLIENT)
    // ====================================================================

    /**
     * Trouve les réservations d'un utilisateur par statut
     */
    List<Booking> findByUserAndBookingStatusOrderByStartDateTimeDesc(User user, BookingStatus status);

    /**
     * Trouve une réservation par son paiement.
     */
    Optional<Booking> findByPayment(Payment payment);

    /**
     * Trouve les réservations à venir pour un utilisateur.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.user = :user
          AND b.startDateTime > :now
          AND b.bookingStatus IN ('CONFIRMED', 'PENDING')
        ORDER BY b.startDateTime
    """)
    List<Booking> findUpcomingBookingsByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * Trouve les réservations passées pour un utilisateur.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.user = :user
          AND b.endDateTime < :now
        ORDER BY b.endDateTime DESC
    """)
    List<Booking> findPastBookingsByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    // ====================================================================
    // CONFLITS / DISPONIBILITÉ
    // ====================================================================

    /**
     * Vérifie s'il existe un conflit de réservation (PENDING + CONFIRMED) sur un intervalle DateTime.
     */
    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM Booking b
        WHERE b.chargingStation = :station
          AND b.bookingStatus IN ('CONFIRMED', 'PENDING')
          AND b.startDateTime < :endDateTime
          AND b.endDateTime > :startDateTime
    """)
    boolean existsConflictingBooking(
            @Param("station") ChargingStation station,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    // ====================================================================
    // JOBS / MAINTENANCE
    // ====================================================================

    /**
     * Trouve les réservations PENDING expirées.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.bookingStatus = 'PENDING'
          AND b.expiresAt < :now
    """)
    List<Booking> findExpiredPendingBookings(@Param("now") Instant now);

    /**
     * Trouve les réservations CONFIRMED à marquer comme COMPLETED.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.bookingStatus = 'CONFIRMED'
          AND b.endDateTime < :cutoffTime
    """)
    List<Booking> findBookingsToComplete(@Param("cutoffTime") LocalDateTime cutoffTime);

    // ====================================================================
    // LIMITES / STATS UTILISATEUR
    // ====================================================================

    /**
     * Compte les réservations actives d'un utilisateur (PENDING + CONFIRMED).
     */
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.user = :user
          AND b.bookingStatus IN ('PENDING', 'CONFIRMED')
    """)
    long countActiveBookingsByUser(@Param("user") User user);

    /**
     * Compte les réservations créées aujourd'hui pour un utilisateur.
     */
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.user = :user
          AND b.createdAt >= :todayStart
    """)
    long countBookingsCreatedTodayByUser(@Param("user") User user, @Param("todayStart") Instant todayStart);

    // ====================================================================
    // DASHBOARD OWNER
    // ====================================================================

    /**
     * Toutes les réservations pour les bornes d'un propriétaire.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.chargingStation.user = :owner
        ORDER BY b.createdAt DESC
    """)
    List<Booking> findByChargingStationUserOrderByCreatedAtDesc(@Param("owner") User owner);

    /**
     * Réservations confirmées d'une borne.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.chargingStation.id = :stationId
          AND b.bookingStatus = 'CONFIRMED'
        ORDER BY b.startDateTime DESC
    """)
    List<Booking> findConfirmedBookingsByStation(@Param("stationId") String stationId);

    /**
     * Revenu total pour un propriétaire.
     */
    @Query("""
        SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b
        WHERE b.chargingStation.user = :owner
          AND b.bookingStatus = 'CONFIRMED'
    """)
    BigDecimal calculateTotalRevenue(@Param("owner") User owner);

    /**
     * Revenu total pour une borne.
     */
    @Query("""
        SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b
        WHERE b.chargingStation.id = :stationId
          AND b.bookingStatus = 'CONFIRMED'
    """)
    BigDecimal calculateRevenueByStation(@Param("stationId") String stationId);

    /**
     * Revenu sur une période (version LocalDateTime).
     */
    @Query("""
        SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b
        WHERE b.chargingStation.user = :owner
          AND b.bookingStatus = 'CONFIRMED'
          AND b.startDateTime >= :startDateTime
          AND b.startDateTime < :endDateTime
    """)
    BigDecimal calculateRevenueBetweenDates(
            @Param("owner") User owner,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    /**
     * Compte le nombre total de réservations pour un propriétaire
     */
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.chargingStation.user = :owner
    """)
    Long countBookingsByOwner(@Param("owner") User owner);

    /**
     * Compte le nombre de réservations par borne
     */
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.chargingStation.id = :stationId
    """)
    Long countBookingsByStation(@Param("stationId") String stationId);

    /**
     * Compte les réservations confirmées par borne
     */
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.chargingStation.id = :stationId
          AND b.bookingStatus = 'CONFIRMED'
    """)
    Long countConfirmedBookingsByStation(@Param("stationId") String stationId);

    /**
     * Compte le nombre de clients uniques pour un propriétaire
     */
    @Query("""
        SELECT COUNT(DISTINCT b.user) FROM Booking b
        WHERE b.chargingStation.user = :owner
          AND b.bookingStatus = 'CONFIRMED'
    """)
    Long countUniqueCustomers(@Param("owner") User owner);

    /**
     * Trouve les bornes les plus rentables d'un propriétaire
     */
    @Query("""
        SELECT b.chargingStation, SUM(b.totalAmount) as revenue
        FROM Booking b
        WHERE b.chargingStation.user = :owner
          AND b.bookingStatus = 'CONFIRMED'
        GROUP BY b.chargingStation
        ORDER BY revenue DESC
    """)
    List<Object[]> findTopStationsByRevenue(@Param("owner") User owner);
}