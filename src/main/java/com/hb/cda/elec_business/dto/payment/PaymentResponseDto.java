package com.hb.cda.elec_business.dto.payment;

import com.hb.cda.elec_business.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {
    private String id;
    private String stripePaymentIntentId;
    private PaymentStatus paymentStatus;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private Instant createdAt;
    private Instant updatedAt;

    // Infos de la réservation liée
    private String bookingId;
    private String bookingStatus;
}
