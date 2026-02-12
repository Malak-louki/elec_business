package com.hb.cda.elec_business.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "booking")
public class Booking extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Date et heure de début de la réservation
     * Timezone : Europe/Paris
     */
    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    /**
     * Date et heure de fin de la réservation
     * Doit être après startDateTime (vérifié en BDD + code)
     */
    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime;

    /**
     * Montant total calculé et dû pour cette réservation
     * Calculé automatiquement : prix_horaire × durée_arrondie
     *
     * NOTE : Ce montant est calculé à la création, AVANT le paiement.
     * Il représente ce que l'utilisateur DOIT payer, pas ce qu'il A payé.
     */
    @Column(name = "total_amount", precision = 7, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    /**
     * Statut actuel de la réservation
     * Cycle : PENDING → CONFIRMED → COMPLETED
     * Alternatives : CANCELLED, EXPIRED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false, length = 20)
    private BookingStatus bookingStatus;

    /**
     * Date limite pour effectuer le paiement
     * Calculé à la création : now + timeout (15 min par défaut)
     * Si dépassé → statut passe à EXPIRED automatiquement
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Utilisateur qui a effectué la réservation
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Paiement associé (null tant que statut = PENDING ou EXPIRED)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    /**
     * Borne de recharge réservée
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "charging_station_id", nullable = false)
    private ChargingStation chargingStation;

    /**
     * Chemin vers la facture PDF générée
     */
    @Column(name = "invoice_path", length = 512)
    private String invoicePath;

    // Constructeurs
    public Booking() {
    }

    public Booking(LocalDateTime startDateTime, LocalDateTime endDateTime,
                   BigDecimal totalAmount, BookingStatus bookingStatus) {
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.totalAmount = totalAmount;
        this.bookingStatus = bookingStatus;
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public String getInvoicePath() {
        return invoicePath;
    }

    public void setInvoicePath(String invoicePath) {
        this.invoicePath = invoicePath;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public ChargingStation getChargingStation() {
        return chargingStation;
    }

    public void setChargingStation(ChargingStation chargingStation) {
        this.chargingStation = chargingStation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Booking booking = (Booking) o;
        return Objects.equals(id, booking.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}