package com.hb.cda.elec_business.service.impl;

import com.hb.cda.elec_business.dto.booking.BookingResponseDto;
import com.hb.cda.elec_business.dto.dashboard.OwnerDashboardStatsDto;
import com.hb.cda.elec_business.dto.dashboard.RevenueAnalyticsDto;
import com.hb.cda.elec_business.dto.dashboard.StationBookingHistoryDto;
import com.hb.cda.elec_business.dto.dashboard.StationPerformanceDto;
import com.hb.cda.elec_business.entity.Booking;
import com.hb.cda.elec_business.entity.BookingStatus;
import com.hb.cda.elec_business.entity.ChargingStation;
import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.exception.BookingValidationException;
import com.hb.cda.elec_business.repository.BookingRepository;
import com.hb.cda.elec_business.repository.ChargingStationRepository;
import com.hb.cda.elec_business.service.BookingService;
import com.hb.cda.elec_business.service.OwnerDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerDashboardServiceImpl implements OwnerDashboardService {

    private final BookingRepository bookingRepository;
    private final ChargingStationRepository stationRepository;
    private final BookingService bookingService;

    // ====================================================================
    // STATISTIQUES GLOBALES
    // ====================================================================

    @Override
    @Transactional(readOnly = true)
    public OwnerDashboardStatsDto getOwnerStatistics(User owner) {
        log.info("Fetching dashboard statistics for owner: {}", owner.getEmail());

        // Récupérer toutes les bornes du propriétaire
        List<ChargingStation> stations = stationRepository.findByUser(owner);

        // Calculer le nombre de bornes disponibles
        long availableStations = stations.stream()
                .filter(ChargingStation::isAvailable)
                .count();

        // Calculer le revenu total
        BigDecimal totalRevenue = bookingService.calculateTotalRevenueForOwner(owner);

        // Calculer le revenu du mois en cours
        LocalDate startOfMonth = YearMonth.now().atDay(1);
        LocalDate endOfMonth = YearMonth.now().atEndOfMonth();
        BigDecimal monthlyRevenue = bookingService.calculateRevenueBetweenDates(owner, startOfMonth, endOfMonth);

        // Compter les réservations
        Long totalBookings = bookingService.countTotalBookingsForOwner(owner);

        // Compter les réservations confirmées
        List<Booking> allBookings = bookingRepository.findByChargingStationUserOrderByCreatedAtDesc(owner);
        long confirmedBookings = allBookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED)
                .count();

        // Compter les réservations à venir
        LocalDateTime now = LocalDateTime.now();
        long upcomingBookings = allBookings.stream()
                .filter(b -> b.getStartDateTime().isAfter(now))
                .filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED || b.getBookingStatus() == BookingStatus.PENDING)
                .count();

        // Compter les clients uniques
        Long uniqueCustomers = bookingService.countUniqueCustomersForOwner(owner);

        // Calculer le taux d'occupation moyen
        Double averageOccupancyRate = calculateAverageOccupancyRate(stations);

        // Calculer le revenu moyen par réservation
        BigDecimal averageRevenuePerBooking = calculateAverageRevenue(totalRevenue, confirmedBookings);

        return OwnerDashboardStatsDto.builder()
                .totalStations(stations.size())
                .availableStations((int) availableStations)
                .totalRevenue(totalRevenue)
                .monthlyRevenue(monthlyRevenue)
                .totalBookings(totalBookings)
                .confirmedBookings(confirmedBookings)
                .upcomingBookings(upcomingBookings)
                .uniqueCustomers(uniqueCustomers)
                .averageOccupancyRate(averageOccupancyRate)
                .averageRevenuePerBooking(averageRevenuePerBooking)
                .build();
    }

    // ====================================================================
    // ANALYTICS DE REVENUS
    // ====================================================================

    @Override
    @Transactional(readOnly = true)
    public RevenueAnalyticsDto getRevenueAnalytics(User owner, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching revenue analytics for owner {} from {} to {}", owner.getEmail(), startDate, endDate);

        // Validation des dates
        if (startDate.isAfter(endDate)) {
            throw new BookingValidationException("La date de début doit être avant la date de fin");
        }

        // Calculer le revenu total sur la période
        BigDecimal totalRevenue = bookingService.calculateRevenueBetweenDates(owner, startDate, endDate);

        // Récupérer toutes les réservations confirmées sur la période
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<Booking> bookings = bookingRepository.findByChargingStationUserOrderByCreatedAtDesc(owner).stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED)
                .filter(b -> !b.getStartDateTime().isBefore(startDateTime) && b.getStartDateTime().isBefore(endDateTime))
                .toList();

        long bookingsCount = bookings.size();

        // Calculer le revenu moyen par jour
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        BigDecimal averageDailyRevenue = daysBetween > 0
                ? totalRevenue.divide(BigDecimal.valueOf(daysBetween), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Calculer le revenu moyen par réservation
        BigDecimal averageRevenuePerBooking = calculateAverageRevenue(totalRevenue, bookingsCount);

        // Générer les revenus par jour
        List<RevenueAnalyticsDto.DailyRevenueDto> dailyRevenues = generateDailyRevenues(bookings, startDate, endDate);

        return RevenueAnalyticsDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalRevenue(totalRevenue)
                .bookingsCount(bookingsCount)
                .averageDailyRevenue(averageDailyRevenue)
                .averageRevenuePerBooking(averageRevenuePerBooking)
                .dailyRevenues(dailyRevenues)
                .build();
    }

    // ====================================================================
    // HISTORIQUE RÉSERVATIONS PAR BORNE
    // ====================================================================

    @Override
    @Transactional(readOnly = true)
    public StationBookingHistoryDto getStationBookingHistory(String stationId, User owner) {
        log.info("Fetching booking history for station {} by owner {}", stationId, owner.getEmail());

        // Vérifier que le propriétaire possède bien cette borne
        ChargingStation station = verifyStationOwnership(stationId, owner);

        // Récupérer toutes les réservations de la borne
        List<BookingResponseDto> bookings = bookingService.getBookingsForStation(stationId, owner);

        // Compter les réservations confirmées
        long confirmedBookings = bookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED)
                .count();

        // Calculer le revenu total
        BigDecimal totalRevenue = bookingService.calculateRevenueForStation(stationId, owner);

        return StationBookingHistoryDto.builder()
                .stationId(stationId)
                .stationName(station.getName())
                .totalBookings((long) bookings.size())
                .confirmedBookings(confirmedBookings)
                .totalRevenue(totalRevenue)
                .bookings(bookings)
                .build();
    }


    // ====================================================================
    // PERFORMANCES PAR BORNE
    // ====================================================================

    @Override
    @Transactional(readOnly = true)
    public StationPerformanceDto getStationPerformance(String stationId, User owner) {
        log.info("Fetching performance for station {} by owner {}", stationId, owner.getEmail());

        // Vérifier la propriété
        ChargingStation station = verifyStationOwnership(stationId, owner);

        return buildStationPerformance(station, owner);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StationPerformanceDto> getAllStationsPerformance(User owner) {
        log.info("Fetching performance for all stations of owner: {}", owner.getEmail());

        List<ChargingStation> stations = stationRepository.findByUser(owner);

        return stations.stream()
                .map(station -> buildStationPerformance(station, owner))
                .sorted(Comparator.comparing(StationPerformanceDto::getTotalRevenue).reversed())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StationPerformanceDto> getTopStationsByRevenue(User owner, int limit) {
        log.info("Fetching top {} stations by revenue for owner: {}", limit, owner.getEmail());

        List<StationPerformanceDto> allPerformances = getAllStationsPerformance(owner);

        return allPerformances.stream()
                .limit(limit)
                .toList();
    }

    // ====================================================================
    // MÉTHODES PRIVÉES UTILITAIRES
    // ====================================================================

    /**
     * Construit le DTO de performance pour une borne
     */
    private StationPerformanceDto buildStationPerformance(ChargingStation station, User owner) {
        String stationId = station.getId();

        // Compter les réservations
        Long totalBookings = bookingService.countBookingsForStation(stationId, owner);
        Long confirmedBookings = bookingRepository.countConfirmedBookingsByStation(stationId);

        // Calculer le revenu
        BigDecimal totalRevenue = bookingService.calculateRevenueForStation(stationId, owner);

        // Revenu moyen par réservation
        BigDecimal averageRevenuePerBooking = calculateAverageRevenue(totalRevenue, confirmedBookings);

        // Taux d'occupation (30 derniers jours)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        Double occupancyRate = bookingService.calculateOccupancyRate(stationId, startDate, endDate);

        // Clients uniques (approximation : compter les utilisateurs distincts dans les bookings)
        List<Booking> stationBookings = bookingRepository.findConfirmedBookingsByStation(stationId);
        long uniqueCustomers = stationBookings.stream()
                .map(Booking::getUser)
                .map(User::getId)
                .distinct()
                .count();

        // Heures totales réservées (approximation)
        long totalBookedHours = stationBookings.stream()
                .mapToLong(b -> java.time.Duration.between(b.getStartDateTime(), b.getEndDateTime()).toHours())
                .sum();

        return StationPerformanceDto.builder()
                .stationId(stationId)
                .stationName(station.getName())
                .hourlyPrice(station.getHourlyPrice())
                .totalBookings(totalBookings)
                .confirmedBookings(confirmedBookings)
                .totalRevenue(totalRevenue)
                .averageRevenuePerBooking(averageRevenuePerBooking)
                .occupancyRate(occupancyRate)
                .uniqueCustomers(uniqueCustomers)
                .totalBookedHours(totalBookedHours)
                .available(station.isAvailable())
                .build();
    }

    /**
     * Calcule le taux d'occupation moyen de toutes les bornes
     */
    private Double calculateAverageOccupancyRate(List<ChargingStation> stations) {
        if (stations.isEmpty()) {
            return 0.0;
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        double totalOccupancy = stations.stream()
                .mapToDouble(station -> bookingService.calculateOccupancyRate(station.getId(), startDate, endDate))
                .sum();

        return totalOccupancy / stations.size();
    }

    /**
     * Calcule le revenu moyen par réservation
     */
    private BigDecimal calculateAverageRevenue(BigDecimal totalRevenue, long bookingsCount) {
        if (bookingsCount == 0) {
            return BigDecimal.ZERO;
        }

        return totalRevenue.divide(BigDecimal.valueOf(bookingsCount), 2, RoundingMode.HALF_UP);
    }

    /**
     * Génère les revenus quotidiens pour une période
     */
    private List<RevenueAnalyticsDto.DailyRevenueDto> generateDailyRevenues(
            List<Booking> bookings,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Map<LocalDate, List<Booking>> bookingsByDate = bookings.stream()
                .collect(Collectors.groupingBy(b -> b.getStartDateTime().toLocalDate()));

        List<RevenueAnalyticsDto.DailyRevenueDto> dailyRevenues = new ArrayList<>();

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            List<Booking> dayBookings = bookingsByDate.getOrDefault(current, Collections.emptyList());

            BigDecimal dayRevenue = dayBookings.stream()
                    .map(Booking::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            dailyRevenues.add(RevenueAnalyticsDto.DailyRevenueDto.builder()
                    .date(current)
                    .revenue(dayRevenue)
                    .bookingsCount((long) dayBookings.size())
                    .build());

            current = current.plusDays(1);
        }

        return dailyRevenues;
    }

    /**
     * Vérifie que l'utilisateur est propriétaire de la borne
     */
    private ChargingStation verifyStationOwnership(String stationId, User owner) {
        ChargingStation station = stationRepository.findById(stationId)
                .orElseThrow(() -> new BookingValidationException("Borne introuvable"));

        if (!station.getUser().getId().equals(owner.getId())) {
            throw new AccessDeniedException("Vous n'êtes pas propriétaire de cette borne");
        }

        return station;
    }
}