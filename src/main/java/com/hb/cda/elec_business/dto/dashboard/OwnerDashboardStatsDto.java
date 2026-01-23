package com.hb.cda.elec_business.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour les statistiques globales du Dashboard Owner
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerDashboardStatsDto {

    /**
     * Nombre total de bornes du propriétaire
     */
    private Integer totalStations;

    /**
     * Nombre de bornes actuellement disponibles
     */
    private Integer availableStations;

    /**
     * Revenu total généré (toutes les réservations confirmées)
     */
    private BigDecimal totalRevenue;

    /**
     * Revenu du mois en cours
     */
    private BigDecimal monthlyRevenue;

    /**
     * Nombre total de réservations
     */
    private Long totalBookings;

    /**
     * Nombre de réservations confirmées
     */
    private Long confirmedBookings;

    /**
     * Nombre de réservations à venir
     */
    private Long upcomingBookings;

    /**
     * Nombre de clients uniques ayant réservé
     */
    private Long uniqueCustomers;

    /**
     * Taux d'occupation moyen (en pourcentage)
     */
    private Double averageOccupancyRate;

    /**
     * Revenu moyen par réservation
     */
    private BigDecimal averageRevenuePerBooking;
}