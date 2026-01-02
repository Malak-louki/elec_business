package com.hb.cda.elec_business.dto.charging;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargingStationRequestDto {

    @NotBlank(message = "Station name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    private String name;

    @NotNull(message = "Hourly price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "999.99", message = "Price must be less than 1000")
    private BigDecimal hourlyPrice;

    /**
     * Puissance numérique en kW (OBLIGATOIRE)
     * Exemples : 7.4, 22, 150
     */
    @NotNull(message = "Charging power is required")
    @DecimalMin(value = "0.1", message = "Charging power must be at least 0.1 kW")
    @DecimalMax(value = "350.0", message = "Charging power must be less than 350 kW")
    private BigDecimal chargingPowerKw;

    @Size(max = 500, message = "Instructions must be less than 500 characters")
    private String instruction;

    private Boolean hasStand;

    @Size(max = 512, message = "Media URL must be less than 512 characters")
    private String mediaUrl;

    @NotNull(message = "Address is required")
    @Valid
    private AddressDto address;

    @NotNull(message = "Location is required")
    @Valid
    private LocationDto location;

    private List<@Valid AvailabilityDto> availabilities;
}