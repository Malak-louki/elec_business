package com.hb.cda.elec_business.service.impl;

import com.hb.cda.elec_business.dto.booking.BookingRequestDto;
import com.hb.cda.elec_business.dto.booking.BookingResponseDto;
import com.hb.cda.elec_business.entity.*;
import com.hb.cda.elec_business.mapper.BookingMapper;
import com.hb.cda.elec_business.repository.BookingRepository;
import com.hb.cda.elec_business.repository.ChargingStationRepository;
import com.hb.cda.elec_business.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ChargingStationRepository stationRepository;

    // ====================================================================
    // GESTION DES RÉSERVATIONS (CLIENTS)
    // ====================================================================

    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto request, User customer) {
        log.info("Creating booking for user {} on station {}", customer.getEmail(), request.getChargingStationId());

        // 1. Récupérer la borne
        ChargingStation station = stationRepository.findByIdWithRelations(request.getChargingStationId())
                .orElseThrow(() -> new IllegalArgumentException("Borne de recharge introuvable avec l'ID: " + request.getChargingStationId()));

        // 2. Vérifier que la borne est disponible
        if (!station.getAvailable()) {
            throw new IllegalStateException("Cette borne n'est pas disponible pour le moment");
        }

        // 3. Valider les dates et heures
        validateBookingDates(request.getStartDate(), request.getEndDate(), request.getStartHour(), request.getEndHour());

        // 4. Vérifier qu'il n'y a pas de conflit de réservation
        boolean hasConflict = bookingRepository.existsConflictingBooking(
                station.getId(),
                request.getStartDate(),
                request.getStartHour(),
                request.getEndDate(),
                request.getEndHour()
        );

        if (hasConflict) {
            throw new IllegalStateException("Ce créneau horaire n'est pas disponible pour cette borne");
        }

        // 5. Calculer le montant à payer
        BigDecimal amount = calculateBookingAmount(
                station.getHourlyPrice(),
                request.getStartDate(),
                request.getStartHour(),
                request.getEndDate(),
                request.getEndHour()
        );

        // 6. Créer la réservation
        Booking booking = new Booking();
        booking.setStartDate(request.getStartDate());
        booking.setEndDate(request.getEndDate());
        booking.setStartHour(request.getStartHour());
        booking.setEndHour(request.getEndHour());
        booking.setPaidAmount(amount);
        booking.setBookingStatus(BookingStatus.PENDING); // En attente de paiement
        booking.setUser(customer);
        booking.setChargingStation(station);

        booking = bookingRepository.save(booking);
        log.info("Booking created successfully with ID: {}", booking.getId());

        return BookingMapper.toResponseDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDto getBookingById(String bookingId, User user) {
        Booking booking = bookingRepository.findByIdWithRelations(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Réservation introuvable avec l'ID: " + bookingId));

        // Vérifier que l'utilisateur a le droit de voir cette réservation
        if (!isUserAuthorizedForBooking(booking, user)) {
            throw new AccessDeniedException("Vous n'avez pas accès à cette réservation");
        }

        return BookingMapper.toResponseDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getMyBookings(User customer) {
        log.info("Fetching all bookings for user: {}", customer.getEmail());
        List<Booking> bookings = bookingRepository.findByUserOrderByStartDateDesc(customer);
        return BookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getMyBookingsByStatus(User customer, BookingStatus status) {
        log.info("Fetching bookings with status {} for user: {}", status, customer.getEmail());
        List<Booking> bookings = bookingRepository.findByUserAndBookingStatusOrderByStartDateDesc(customer, status);
        return BookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional
    public void cancelBooking(String bookingId, User user) {
        log.info("Cancelling booking {} by user {}", bookingId, user.getEmail());

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Réservation introuvable"));

        // Vérifier les autorisations
        if (!isUserAuthorizedForBooking(booking, user)) {
            throw new AccessDeniedException("Vous n'avez pas le droit d'annuler cette réservation");
        }

        // Vérifier qu'on peut encore annuler (pas déjà passée)
        LocalDateTime bookingStart = LocalDateTime.of(booking.getStartDate(), booking.getStartHour());
        if (bookingStart.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Impossible d'annuler une réservation passée");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Booking {} cancelled successfully", bookingId);
    }

    @Override
    @Transactional
    public BookingResponseDto updateBookingStatus(String bookingId, BookingStatus newStatus) {
        log.info("Updating booking {} status to {}", bookingId, newStatus);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Réservation introuvable"));

        booking.setBookingStatus(newStatus);
        booking = bookingRepository.save(booking);

        return BookingMapper.toResponseDto(booking);
    }

    // ====================================================================
    // VÉRIFICATION DE DISPONIBILITÉ
    // ====================================================================

    @Override
    @Transactional(readOnly = true)
    public boolean isSlotAvailable(String stationId, LocalDate startDate, LocalDate endDate,
                                   String startHourStr, String endHourStr) {
        LocalTime startHour = LocalTime.parse(startHourStr);
        LocalTime endHour = LocalTime.parse(endHourStr);

        return !bookingRepository.existsConflictingBooking(stationId, startDate, startHour, endDate, endHour);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getBookedSlotsForStation(String stationId, LocalDate startDate, LocalDate endDate) {
        LocalTime midnight = LocalTime.of(0, 0);
        LocalTime endOfDay = LocalTime.of(23, 59);

        List<Booking> bookings = bookingRepository.findConflictingBookings(
                stationId, startDate, midnight, endDate, endOfDay
        );

        return BookingMapper.toResponseDtoList(bookings);
    }

    // ====================================================================
    // MÉTHODES POUR LE DASHBOARD OWNER
    // ====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getAllBookingsForOwner(User owner) {
        log.info("Fetching all bookings for owner: {}", owner.getEmail());
        List<Booking> bookings = bookingRepository.findByChargingStationUserOrderByCreatedAtDesc(owner);
        return BookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getBookingsForStation(String stationId, User owner) {
        log.info("Fetching bookings for station {} by owner {}", stationId, owner.getEmail());

        // Vérifier que l'utilisateur est bien propriétaire de cette borne
        verifyStationOwnership(stationId, owner);

        List<Booking> bookings = bookingRepository.findByChargingStationIdOrderByStartDateDesc(stationId);
        return BookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getConfirmedBookingsForStation(String stationId, User owner) {
        log.info("Fetching confirmed bookings for station {} by owner {}", stationId, owner.getEmail());

        verifyStationOwnership(stationId, owner);

        List<Booking> bookings = bookingRepository.findConfirmedBookingsByStation(stationId);
        return BookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getUpcomingBookingsForOwner(User owner) {
        log.info("Fetching upcoming bookings for owner: {}", owner.getEmail());
        LocalDate today = LocalDate.now();
        List<Booking> bookings = bookingRepository.findUpcomingBookingsByOwner(owner, today);
        return BookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getPastBookingsForOwner(User owner) {
        log.info("Fetching past bookings for owner: {}", owner.getEmail());
        LocalDate today = LocalDate.now();
        List<Booking> bookings = bookingRepository.findPastBookingsByOwner(owner, today);
        return BookingMapper.toResponseDtoList(bookings);
    }

    // ====================================================================
    // STATISTIQUES ET ANALYTICS
    // ====================================================================

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalRevenueForOwner(User owner) {
        log.info("Calculating total revenue for owner: {}", owner.getEmail());
        return bookingRepository.calculateTotalRevenue(owner);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateRevenueForStation(String stationId, User owner) {
        log.info("Calculating revenue for station {} by owner {}", stationId, owner.getEmail());

        verifyStationOwnership(stationId, owner);

        return bookingRepository.calculateRevenueByStation(stationId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateRevenueBetweenDates(User owner, LocalDate startDate, LocalDate endDate) {
        log.info("Calculating revenue between {} and {} for owner {}", startDate, endDate, owner.getEmail());
        return bookingRepository.calculateRevenueBetweenDates(owner, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countTotalBookingsForOwner(User owner) {
        log.info("Counting total bookings for owner: {}", owner.getEmail());
        return bookingRepository.countBookingsByOwner(owner);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countBookingsForStation(String stationId, User owner) {
        log.info("Counting bookings for station {} by owner {}", stationId, owner.getEmail());

        verifyStationOwnership(stationId, owner);

        return bookingRepository.countBookingsByStation(stationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countUniqueCustomersForOwner(User owner) {
        log.info("Counting unique customers for owner: {}", owner.getEmail());
        return bookingRepository.countUniqueCustomers(owner);
    }

    // ====================================================================
    // MÉTHODES PRIVÉES UTILITAIRES
    // ====================================================================

    /**
     * Valide que les dates et heures de réservation sont cohérentes
     */
    private void validateBookingDates(LocalDate startDate, LocalDate endDate,
                                      LocalTime startHour, LocalTime endHour) {
        LocalDateTime start = LocalDateTime.of(startDate, startHour);
        LocalDateTime end = LocalDateTime.of(endDate, endHour);

        if (start.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La date de début ne peut pas être dans le passé");
        }

        if (end.isBefore(start) || end.isEqual(start)) {
            throw new IllegalArgumentException("La date/heure de fin doit être après la date/heure de début");
        }

        // Vérifier que la réservation ne dépasse pas 7 jours
        long daysBetween = Duration.between(start, end).toDays();
        if (daysBetween > 7) {
            throw new IllegalArgumentException("La durée maximale de réservation est de 7 jours");
        }
    }

    /**
     * Calcule le montant total de la réservation en fonction du tarif horaire
     */
    private BigDecimal calculateBookingAmount(BigDecimal hourlyPrice,
                                              LocalDate startDate, LocalTime startHour,
                                              LocalDate endDate, LocalTime endHour) {
        LocalDateTime start = LocalDateTime.of(startDate, startHour);
        LocalDateTime end = LocalDateTime.of(endDate, endHour);

        // Calculer la durée en heures (arrondi au supérieur)
        long minutes = Duration.between(start, end).toMinutes();
        double hours = Math.ceil(minutes / 60.0);

        BigDecimal totalAmount = hourlyPrice.multiply(BigDecimal.valueOf(hours));

        log.info("Calculated booking amount: {} (hourly rate: {}, duration: {} hours)",
                totalAmount, hourlyPrice, hours);

        return totalAmount;
    }

    /**
     * Vérifie si un utilisateur a le droit d'accéder à une réservation
     * (soit il est le client, soit il est le propriétaire de la borne)
     */
    private boolean isUserAuthorizedForBooking(Booking booking, User user) {
        // L'utilisateur est le client qui a fait la réservation
        if (booking.getUser().getId().equals(user.getId())) {
            return true;
        }

        // L'utilisateur est le propriétaire de la borne
        if (booking.getChargingStation().getUser().getId().equals(user.getId())) {
            return true;
        }

        return false;
    }

    /**
     * Vérifie que l'utilisateur est bien propriétaire de la borne
     */
    private void verifyStationOwnership(String stationId, User owner) {
        ChargingStation station = stationRepository.findById(stationId)
                .orElseThrow(() -> new IllegalArgumentException("Borne introuvable"));

        if (!station.getUser().getId().equals(owner.getId())) {
            throw new AccessDeniedException("Vous n'êtes pas propriétaire de cette borne");
        }
    }
}