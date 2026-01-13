package com.hb.cda.elec_business.dto.booking;

import com.hb.cda.elec_business.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {

    private String id;

    /**
     * Date et heure de début de la réservation
     */
    private LocalDateTime startDateTime;

    /**
     * Date et heure de fin de la réservation
     */
    private LocalDateTime endDateTime;

    /**
     * Montant total calculé et dû
     * NOTE : C'est le montant que l'utilisateur DOIT payer,
     * pas forcément ce qu'il A DÉJÀ payé.
     */
    private BigDecimal totalAmount;

    private BookingStatus bookingStatus;
    private String invoicePath;

    // Timestamps d'audit
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Date limite pour payer (si PENDING)
     * null si déjà CONFIRMED/CANCELLED/EXPIRED/COMPLETED
     */
    private Instant expiresAt;

    // Informations liées
    private UserInfoDto user;
    private StationInfoDto chargingStation;
    private PaymentInfoDto payment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoDto {
        private String id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StationInfoDto {
        private String id;
        private String name;
        private BigDecimal hourlyPrice;
        private String chargingPower;
        private String address;
        private String city;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfoDto {
        private String id;
        private String stripePaymentIntentId;
        private String paymentStatus;
    }
}