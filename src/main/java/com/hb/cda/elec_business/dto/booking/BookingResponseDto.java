package com.hb.cda.elec_business.dto.booking;

import com.hb.cda.elec_business.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO pour retourner les informations d'une réservation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {

    private String id;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startHour;
    private LocalTime endHour;
    private BigDecimal paidAmount;
    private BookingStatus bookingStatus;
    private String invoicePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Informations sur la borne
    private ChargingStationSummaryDto chargingStation;

    // Informations sur le client (pour le dashboard owner)
    private UserSummaryDto customer;

    // Informations sur le paiement
    private PaymentSummaryDto payment;

    /**
     * Sous-DTO pour résumer les infos de la borne
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChargingStationSummaryDto {
        private String id;
        private String name;
        private BigDecimal hourlyPrice;
        private String chargingPower;
        private String address;
    }

    /**
     * Sous-DTO pour résumer les infos du client
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummaryDto {
        private String id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
    }

    /**
     * Sous-DTO pour résumer les infos du paiement
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSummaryDto {
        private String id;
        private String stripePaymentIntentId;
        private String paymentStatus;
    }
}