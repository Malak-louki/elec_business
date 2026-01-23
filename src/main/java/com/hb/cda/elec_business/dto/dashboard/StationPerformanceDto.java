package com.hb.cda.elec_business.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour les KPIs de performance d'une borne
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationPerformanceDto {

    /**
     * ID de la borne
     */
    private String stationId;

    /**
     * Nom de la borne
     */
    private String stationName;

    /**
     * Tarif horaire
     */
    private BigDecimal hourlyPrice;

    /**
     * Nombre total de réservations
     */
    private Long totalBookings;

    /**
     * Nombre de réservations confirmées
     */
    private Long confirmedBookings;

    /**
     * Revenu total généré
     */
    private BigDecimal totalRevenue;

    /**
     * Revenu moyen par réservation
     */
    private BigDecimal averageRevenuePerBooking;

    /**
     * Taux d'occupation (en pourcentage)
     * Calculé sur les 30 derniers jours par défaut
     */
    private Double occupancyRate;

    /**
     * Nombre de clients uniques
     */
    private Long uniqueCustomers;

    /**
     * Nombre d'heures totales réservées
     */
    private Long totalBookedHours;

    /**
     * Disponibilité (true si la borne est active)
     */
    private Boolean available;
}