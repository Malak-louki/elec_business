package com.hb.cda.elec_business.dto.dashboard;

import com.hb.cda.elec_business.dto.booking.BookingResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO pour l'historique des réservations d'une borne spécifique
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationBookingHistoryDto {

    /**
     * ID de la borne
     */
    private String stationId;

    /**
     * Nom de la borne
     */
    private String stationName;

    /**
     * Nombre total de réservations
     */
    private Long totalBookings;

    /**
     * Nombre de réservations confirmées
     */
    private Long confirmedBookings;

    /**
     * Revenu total généré par cette borne
     */
    private BigDecimal totalRevenue;

    /**
     * Liste des réservations (limitée ou complète selon le besoin)
     */
    private List<BookingResponseDto> bookings;
}