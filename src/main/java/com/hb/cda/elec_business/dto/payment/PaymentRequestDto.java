package com.hb.cda.elec_business.dto.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {

    @NotBlank(message = "Booking ID is required")
    private String bookingId;

    /**
     * Méthode de paiement simulée
     * Valeurs possibles : "card", "paypal", "apple_pay", "google_pay"
     */
    private String paymentMethod = "card";

    /**
     * Simule un succès ou échec de paiement
     * true = succès (défaut), false = échec
     */
    private Boolean simulateSuccess = true;


}
