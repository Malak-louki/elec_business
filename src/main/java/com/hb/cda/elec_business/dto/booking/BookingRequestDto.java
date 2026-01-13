package com.hb.cda.elec_business.dto.booking;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDto {

    @NotBlank(message = "L'ID de la borne est obligatoire")
    private String chargingStationId;

    /**
     * Date et heure de début
     * Format ISO-8601 : "2026-01-15T14:00:00"
     */
    @NotNull(message = "La date de début est obligatoire")
    @Future(message = "La date de début doit être dans le futur")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDateTime;

    /**
     * Date et heure de fin
     * Format ISO-8601 : "2026-01-15T16:00:00"
     */
    @NotNull(message = "La date de fin est obligatoire")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDateTime;
}