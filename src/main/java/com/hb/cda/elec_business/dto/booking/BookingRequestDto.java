package com.hb.cda.elec_business.dto.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO pour créer une nouvelle réservation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDto {

    @NotBlank(message = "L'ID de la borne de recharge est requis")
    private String chargingStationId;

    @NotNull(message = "La date de début est requise")
    @Future(message = "La date de début doit être dans le futur")
    private LocalDate startDate;

    @NotNull(message = "La date de fin est requise")
    private LocalDate endDate;

    @NotNull(message = "L'heure de début est requise")
    private LocalTime startHour;

    @NotNull(message = "L'heure de fin est requise")
    private LocalTime endHour;

    // Optionnel : pour des informations supplémentaires
    private String notes;
}