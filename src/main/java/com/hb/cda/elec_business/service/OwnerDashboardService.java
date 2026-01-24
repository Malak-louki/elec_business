package com.hb.cda.elec_business.service;

import com.hb.cda.elec_business.dto.dashboard.OwnerDashboardStatsDto;
import com.hb.cda.elec_business.dto.dashboard.RevenueAnalyticsDto;
import com.hb.cda.elec_business.dto.dashboard.StationBookingHistoryDto;
import com.hb.cda.elec_business.dto.dashboard.StationPerformanceDto;
import com.hb.cda.elec_business.entity.User;

import java.time.LocalDate;
import java.util.List;

/**
 * Service pour le Dashboard Owner
 * Fournit des statistiques et analytics pour les propriétaires de bornes
 */
public interface OwnerDashboardService {

    /**
     * Récupère les statistiques globales du propriétaire
     * Vue d'ensemble de toutes ses bornes et réservations
     */
    OwnerDashboardStatsDto getOwnerStatistics(User owner);

    /**
     * Récupère les analytics de revenus sur une période donnée
     * @param owner Propriétaire
     * @param startDate Date de début (incluse)
     * @param endDate Date de fin (incluse)
     * @return Analytics détaillés avec revenus par jour
     */
    RevenueAnalyticsDto getRevenueAnalytics(User owner, LocalDate startDate, LocalDate endDate);

    /**
     * Récupère l'historique des réservations pour une borne spécifique
     * @param stationId ID de la borne
     * @param owner Propriétaire (pour vérification)
     * @return Historique avec toutes les réservations
     */
    StationBookingHistoryDto getStationBookingHistory(String stationId, User owner);

    /**
     * Récupère les performances (KPIs) d'une borne spécifique
     * @param stationId ID de la borne
     * @param owner Propriétaire (pour vérification)
     * @return KPIs de la borne
     */
    StationPerformanceDto getStationPerformance(String stationId, User owner);

    /**
     * Récupère les performances de toutes les bornes du propriétaire
     * @param owner Propriétaire
     * @return Liste des KPIs pour chaque borne
     */
    List<StationPerformanceDto> getAllStationsPerformance(User owner);

    /**
     * Récupère les N meilleures bornes par revenu
     * @param owner Propriétaire
     * @param limit Nombre de bornes à retourner (par défaut 5)
     * @return Top N des bornes les plus rentables
     */
    List<StationPerformanceDto> getTopStationsByRevenue(User owner, int limit);
}