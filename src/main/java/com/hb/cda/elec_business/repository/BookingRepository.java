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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    /**
     * Vérifie si un créneau est déjà réservé (version date + heure).
     * (Overload volontaire : signature différente, donc pas un doublon Java)
     */
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.chargingStation.id = :stationId
          AND b.bookingStatus IN ('PENDING', 'CONFIRMED')
          AND NOT (
             (b.endDate < :startDate) OR
             (b.startDate > :endDate) OR
             (b.startDate = :endDate AND b.startHour >= :endHour) OR
             (b.endDate = :startDate AND b.endHour <= :startHour)
          )
    """)
    boolean existsConflictingBooking(
            @Param("stationId") String stationId,
            @Param("startDate") LocalDate startDate,
            @Param("startHour") LocalTime startHour,
            @Param("endDate") LocalDate endDate,
            @Param("endHour") LocalTime endHour
    );

    /**
     * Trouve les réservations qui chevauchent une période donnée (version date + heure).
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.chargingStation.id = :stationId
          AND b.bookingStatus IN ('PENDING', 'CONFIRMED')
          AND NOT (
             (b.endDate < :startDate) OR
             (b.startDate > :endDate) OR
             (b.startDate = :endDate AND b.startHour >= :endHour) OR
             (b.endDate = :startDate AND b.endHour <= :startHour)
          )
    """)
    List<Booking> findConflictingBookings(
            @Param("stationId") String stationId,
            @Param("startDate") LocalDate startDate,
            @Param("startHour") LocalTime startHour,
            @Param("endDate") LocalDate endDate,
            @Param("endHour") LocalTime endHour
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
     * Réservations d'une borne spécifique (tri legacy date+heure).
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.chargingStation.id = :stationId
        ORDER BY b.startDate DESC, b.startHour DESC
    """)
    List<Booking> findByChargingStationIdOrderByStartDateDesc(@Param("stationId") String stationId);

    /**
     * Réservations confirmées d'une borne.
     */
    @Query("""
        SELECT b FROM Booking b
        WHERE b.chargingStation.id = :stationId
          AND b.bookingStatus = 'CONFIRMED'
        ORDER BY b.startDate DESC
    """)
    List<Booking> findConfirmedBookingsByStation(@Param("stationId") String stationId);

    /**
     * Revenu total pour un propriétaire.
     */
    @Query("""
        SELECT COALESCE(SUM(b.paidAmount), 0) FROM Booking b
        WHERE b.chargingStation.user = :owner
          AND b.bookingStatus = 'CONFIRMED'
    """)
    BigDecimal calculateTotalRevenue(@Param("owner") User owner);

    /**
     * Revenu total pour une borne.
     */
    @Query("""
        SELECT COALESCE(SUM(b.paidAmount), 0) FROM Booking b
        WHERE b.chargingStation.id = :stationId
          AND b.bookingStatus = 'CONFIRMED'
    """)
    BigDecimal calculateRevenueByStation(@Param("stationId") String stationId);

    /**
     * Revenu sur une période (version startDate/endDate).
     */
    @Query("""
        SELECT COALESCE(SUM(b.paidAmount), 0) FROM Booking b
        WHERE b.chargingStation.user = :owner
          AND b.bookingStatus = 'CONFIRMED'
          AND b.startDate BETWEEN :startDate AND :endDate
    """)
    BigDecimal calculateRevenueBetweenDates(
            @Param("owner") User owner,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.chargingStation.user = :owner
    """)
    Long countBookingsByOwner(@Param("owner") User owner);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.chargingStation.id = :stationId
    """)
    Long countBookingsByStation(@Param("stationId") String stationId);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.chargingStation.id = :stationId
          AND b.bookingStatus = 'CONFIRMED'
    """)
    Long countConfirmedBookingsByStation(@Param("stationId") String stationId);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.chargingStation.user = :owner
          AND b.startDate >= :today
          AND b.bookingStatus IN ('PENDING', 'CONFIRMED')
        ORDER BY b.startDate ASC, b.startHour ASC
    """)
    List<Booking> findUpcomingBookingsByOwner(@Param("owner") User owner, @Param("today") LocalDate today);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.chargingStation.user = :owner
          AND b.endDate < :today
        ORDER BY b.endDate DESC
    """)
    List<Booking> findPastBookingsByOwner(@Param("owner") User owner, @Param("today") LocalDate today);

    // ====================================================================
    // STATS AVANCÉES
    // ====================================================================

    @Query("""
        SELECT COALESCE(SUM(
            TIMESTAMPDIFF(HOUR,
               CONCAT(b.startDate, ' ', b.startHour),
               CONCAT(b.endDate, ' ', b.endHour)
            )
        ), 0)
        FROM Booking b
        WHERE b.chargingStation.id = :stationId
          AND b.bookingStatus = 'CONFIRMED'
    """)
    Long calculateTotalBookedHours(@Param("stationId") String stationId);

    @Query("""
        SELECT b.chargingStation, SUM(b.paidAmount) as revenue
        FROM Booking b
        WHERE b.chargingStation.user = :owner
          AND b.bookingStatus = 'CONFIRMED'
        GROUP BY b.chargingStation
        ORDER BY revenue DESC
    """)
    List<Object[]> findTopStationsByRevenue(@Param("owner") User owner);

    @Query("""
        SELECT COUNT(DISTINCT b.user) FROM Booking b
        WHERE b.chargingStation.user = :owner
          AND b.bookingStatus = 'CONFIRMED'
    """)
    Long countUniqueCustomers(@Param("owner") User owner);
}
