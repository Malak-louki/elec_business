package com.hb.cda.elec_business.controller;

import com.hb.cda.elec_business.dto.dashboard.OwnerDashboardStatsDto;
import com.hb.cda.elec_business.dto.dashboard.RevenueAnalyticsDto;
import com.hb.cda.elec_business.dto.dashboard.StationBookingHistoryDto;
import com.hb.cda.elec_business.dto.dashboard.StationPerformanceDto;
import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.service.OwnerDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/owner/dashboard")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")  // Ajouté pour éviter les problèmes CORS
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class OwnerDashboardController {

    private final OwnerDashboardService dashboardService;

    /**
     * Récupère les statistiques globales du propriétaire
     * GET /api/owner/dashboard/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<OwnerDashboardStatsDto> getOwnerStatistics(
            @AuthenticationPrincipal User owner
    ) {
        log.info("📊 Fetching dashboard stats for owner: {}", owner.getEmail());

        try {
            OwnerDashboardStatsDto stats = dashboardService.getOwnerStatistics(owner);
            log.info("✅ Stats retrieved successfully: {} stations, {} total revenue",
                    stats.getTotalStations(), stats.getTotalRevenue());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("❌ Error fetching stats for owner {}: {}", owner.getEmail(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Récupère les analytics de revenus sur une période
     * GET /api/owner/dashboard/revenue/analytics?startDate=2024-01-01&endDate=2024-01-31
     */
    @GetMapping("/revenue/analytics")
    public ResponseEntity<RevenueAnalyticsDto> getRevenueAnalytics(
            @AuthenticationPrincipal User owner,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("💰 Fetching revenue analytics for owner {} from {} to {}",
                owner.getEmail(), startDate, endDate);

        try {
            RevenueAnalyticsDto analytics = dashboardService.getRevenueAnalytics(owner, startDate, endDate);
            log.info("✅ Revenue analytics retrieved: total revenue = {}", analytics.getTotalRevenue());
            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("❌ Error fetching revenue analytics: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Récupère l'historique des réservations d'une borne
     * GET /api/owner/dashboard/stations/{stationId}/bookings
     */
    @GetMapping("/stations/{stationId}/bookings")
    public ResponseEntity<StationBookingHistoryDto> getStationBookingHistory(
            @PathVariable String stationId,
            @AuthenticationPrincipal User owner
    ) {
        log.info("📅 Fetching booking history for station {} by owner {}", stationId, owner.getEmail());

        try {
            StationBookingHistoryDto history = dashboardService.getStationBookingHistory(stationId, owner);
            log.info("✅ Booking history retrieved: {} total bookings", history.getTotalBookings());
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("❌ Error fetching booking history: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Récupère les performances d'une borne spécifique
     * GET /api/owner/dashboard/stations/{stationId}/performance
     */
    @GetMapping("/stations/{stationId}/performance")
    public ResponseEntity<StationPerformanceDto> getStationPerformance(
            @PathVariable String stationId,
            @AuthenticationPrincipal User owner
    ) {
        log.info("📈 Fetching performance for station {} by owner {}", stationId, owner.getEmail());

        try {
            StationPerformanceDto performance = dashboardService.getStationPerformance(stationId, owner);
            log.info("✅ Performance retrieved: {} bookings, {} revenue",
                    performance.getTotalBookings(), performance.getTotalRevenue());
            return ResponseEntity.ok(performance);
        } catch (Exception e) {
            log.error("❌ Error fetching station performance: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Récupère les performances de toutes les bornes
     * GET /api/owner/dashboard/stations/performance
     */
    @GetMapping("/stations/performance")
    public ResponseEntity<List<StationPerformanceDto>> getAllStationsPerformance(
            @AuthenticationPrincipal User owner
    ) {
        log.info("📊 Fetching all stations performance for owner: {}", owner.getEmail());

        try {
            List<StationPerformanceDto> performances = dashboardService.getAllStationsPerformance(owner);
            log.info("✅ Retrieved performance for {} stations", performances.size());
            return ResponseEntity.ok(performances);
        } catch (Exception e) {
            log.error("❌ Error fetching all stations performance: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Récupère le top N des bornes par revenu
     * GET /api/owner/dashboard/stations/top?limit=5
     */
    @GetMapping("/stations/top")
    public ResponseEntity<List<StationPerformanceDto>> getTopStationsByRevenue(
            @AuthenticationPrincipal User owner,
            @RequestParam(defaultValue = "5") int limit
    ) {
        log.info("🏆 Fetching top {} stations for owner: {}", limit, owner.getEmail());

        try {
            List<StationPerformanceDto> topStations = dashboardService.getTopStationsByRevenue(owner, limit);
            log.info("✅ Retrieved {} top stations", topStations.size());
            return ResponseEntity.ok(topStations);
        } catch (Exception e) {
            log.error("❌ Error fetching top stations: {}", e.getMessage(), e);
            throw e;
        }
    }
}