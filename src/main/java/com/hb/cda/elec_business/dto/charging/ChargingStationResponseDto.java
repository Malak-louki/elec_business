package com.hb.cda.elec_business.dto.charging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargingStationResponseDto {

    private String id;
    private String name;
    private BigDecimal hourlyPrice;

    /**
     * Puissance numérique en kW (pour calculs côté frontend)
     */
    private BigDecimal chargingPowerKw;

    /**
     * Puissance avec unité (pour affichage)
     */
    private String chargingPower;

    private String instruction;
    private Boolean hasStand;
    private String mediaUrl;
    private Boolean available;
    private Instant createdAt;
    private Instant updatedAt;

    private AddressDto address;
    private LocationDto location;
    private List<AvailabilityDto> availabilities;
    private OwnerInfoDto owner;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OwnerInfoDto {
        private String id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
    }
}