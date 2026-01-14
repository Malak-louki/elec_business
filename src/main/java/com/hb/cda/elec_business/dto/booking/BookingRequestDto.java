package com.hb.cda.elec_business.dto.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO pour créer une nouvelle réservation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDto {

    @NotBlank(message = "L'ID de la borne de recharge est requis")
    private String chargingStationId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    // Optionnel : pour des informations supplémentaires
    private String notes;
}