package com.hb.cda.elec_business.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO pour les analytics de revenus sur une période
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueAnalyticsDto {

    /**
     * Date de début de la période analysée
     */
    private LocalDate startDate;

    /**
     * Date de fin de la période analysée
     */
    private LocalDate endDate;

    /**
     * Revenu total sur la période
     */
    private BigDecimal totalRevenue;

    /**
     * Nombre de réservations sur la période
     */
    private Long bookingsCount;

    /**
     * Revenu moyen par jour
     */
    private BigDecimal averageDailyRevenue;

    /**
     * Revenu moyen par réservation
     */
    private BigDecimal averageRevenuePerBooking;

    /**
     * Revenus par jour (pour graphiques)
     */
    private List<DailyRevenueDto> dailyRevenues;

    /**
     * Sous-DTO pour le revenu d'un jour spécifique
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenueDto {
        private LocalDate date;
        private BigDecimal revenue;
        private Long bookingsCount;
    }
}