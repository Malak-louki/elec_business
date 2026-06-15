package com.hb.cda.elec_business.service.impl;

import com.hb.cda.elec_business.dto.booking.BookingRequestDto;
import com.hb.cda.elec_business.dto.booking.BookingResponseDto;
import com.hb.cda.elec_business.entity.*;
import com.hb.cda.elec_business.exception.BookingConflictException;
import com.hb.cda.elec_business.exception.BookingValidationException;
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
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ChargingStationRepository stationRepository;

    // Timeout pour le paiement (15 minutes par défaut)
    private static final int PAYMENT_TIMEOUT_MINUTES = 15;

    // ====================================================================
    // GESTION DES RÉSERVATIONS (CLIENTS)
    // ====================================================================

    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto request, User customer) {

        ChargingStation station = stationRepository.findByIdWithRelations(request.getChargingStationId())
                .orElseThrow(() -> new BookingValidationException("Borne de recharge introuvable avec l'ID: " + request.getChargingStationId()));

        if (!station.isAvailable()) {
            throw new BookingValidationException("Cette borne n'est pas disponible pour le moment");
        }

        validateBookingDateTime(request.getStartDateTime(), request.getEndDateTime());

        boolean hasConflict = bookingRepository.existsConflictingBooking(
                station,
                request.getStartDateTime(),
                request.getEndDateTime()
        );

        if (hasConflict) {
            throw new BookingConflictException("Ce créneau horaire n'est pas disponible pour cette borne");
        }

        BigDecimal totalAmount = calculateBookingAmount(
                station.getHourlyPrice(),
                request.getStartDateTime(),
                request.getEndDateTime()
        );

        //  Calculer la date d'expiration (maintenant + 15 minutes)
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(PAYMENT_TIMEOUT_MINUTES));

        Booking booking = new Booking();
        booking.setStartDateTime(request.getStartDateTime());
        booking.setEndDateTime(request.getEndDateTime());
        booking.setTotalAmount(totalAmount);
        booking.setBookingStatus(BookingStatus.PENDING);
        booking.setExpiresAt(expiresAt);
        booking.setUser(customer);
        booking.setChargingStation(station);

        booking = bookingRepository.save(booking);

        return BookingMapper.toResponseDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDto getBookingById(String bookingId, User user) {
        Booking booking = bookingRepository.findByIdWithRelations(bookingId)
                .orElseThrow(() -> new BookingValidationException("Réservation introuvable avec l'ID: " + bookingId));

        if (!isUserAuthorizedForBooking(booking, user)) {
            throw new AccessDeniedException("Vous n'avez pas accès à cette réservation");
        }

        return BookingMapper.toResponseDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getMyBookings(User customer) {
        log.info("Fetching all bookings for user: {}", customer.getEmail());
        List<Booking> bookings = bookingRepository.findByUserWithRelations(customer);
        return BookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getMyBookingsByStatus(User customer, BookingStatus status) {
        log.info("Fetching bookings with status {} for user: {}", status, customer.getEmail());
        List<Booking> bookings = bookingRepository.findByUserAndBookingStatusOrderByStartDateTimeDesc(customer, status);
        return BookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional
    public void cancelBooking(String bookingId, User user) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingValidationException("Réservation introuvable"));

        if (!isUserAuthorizedForBooking(booking, user)) {
            throw new AccessDeniedException("Vous n'avez pas le droit d'annuler cette réservation");
        }

        if (booking.getStartDateTime().isBefore(LocalDateTime.now())) {
            throw new BookingValidationException("Impossible d'annuler une réservation passée");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

    }

    @Override
    @Transactional
    public BookingResponseDto updateBookingStatus(String bookingId, BookingStatus newStatus) {
        log.info("Updating booking {} status to {}", bookingId, newStatus);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingValidationException("Réservation introuvable"));

        booking.setBookingStatus(newStatus);
        booking = bookingRepository.save(booking);

        return BookingMapper.toResponseDto(booking);
    }


    @Override
    @Transactional(readOnly = true)
    public boolean isSlotAvailable(String stationId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        ChargingStation station = stationRepository.findById(stationId)
                .orElseThrow(() -> new BookingValidationException("Borne introuvable"));

        return !bookingRepository.existsConflictingBooking(station, startDateTime, endDateTime);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getBookedSlotsForStation(String stationId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        ChargingStation station = stationRepository.findById(stationId)
                .orElseThrow(() -> new BookingValidationException("Borne introuvable"));

        List<Booking> bookings = bookingRepository.findByChargingStationWithRelations(station);

        // Filtrer les bookings dans la période demandée
        List<Booking> filteredBookings = bookings.stream()
                .filter(b -> !b.getEndDateTime().isBefore(startDateTime) && !b.getStartDateTime().isAfter(endDateTime))
                .filter(b -> b.getBookingStatus() == BookingStatus.PENDING || b.getBookingStatus() == BookingStatus.CONFIRMED)
                .toList();

        return BookingMapper.toResponseDtoList(filteredBookings);
    }


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

        verifyStationOwnership(stationId, owner);

        ChargingStation station = stationRepository.findById(stationId).orElseThrow();
        List<Booking> bookings = bookingRepository.findByChargingStationWithRelations(station);

        return BookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getConfirmedBookingsForStation(String stationId, User owner) {
        verifyStationOwnership(stationId, owner);

        List<Booking> bookings = bookingRepository.findConfirmedBookingsByStation(stationId);
        return BookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getUpcomingBookingsForOwner(User owner) {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = bookingRepository.findUpcomingBookingsByUser(owner, now);
        return BookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getPastBookingsForOwner(User owner) {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = bookingRepository.findPastBookingsByUser(owner, now);
        return BookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalRevenueForOwner(User owner) {;
        return bookingRepository.calculateTotalRevenue(owner);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateRevenueForStation(String stationId, User owner) {

        verifyStationOwnership(stationId, owner);

        return bookingRepository.calculateRevenueByStation(stationId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateRevenueBetweenDates(User owner, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        return bookingRepository.calculateRevenueBetweenDates(owner, startDateTime, endDateTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countTotalBookingsForOwner(User owner) {
        return bookingRepository.countBookingsByOwner(owner);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countBookingsForStation(String stationId, User owner) {
        verifyStationOwnership(stationId, owner);

        return bookingRepository.countBookingsByStation(stationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countUniqueCustomersForOwner(User owner) {
        return bookingRepository.countUniqueCustomers(owner);
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateOccupancyRate(String stationId, LocalDate startDate, LocalDate endDate) {
        long totalHours = Duration.between(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay()
        ).toHours();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<Booking> bookings = bookingRepository.findConfirmedBookingsByStation(stationId);

        // Calculer le nombre d'heures réservées
        long bookedHours = bookings.stream()
                .filter(b -> !b.getEndDateTime().isBefore(startDateTime) && !b.getStartDateTime().isAfter(endDateTime))
                .mapToLong(b -> Duration.between(b.getStartDateTime(), b.getEndDateTime()).toHours())
                .sum();

        // Calculer le taux d'occupation
        if (totalHours == 0) {
            return 0.0;
        }

        return (bookedHours * 100.0) / totalHours;
    }

    private void validateBookingDateTime(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime.isBefore(LocalDateTime.now())) {
            throw new BookingValidationException("La date de début ne peut pas être dans le passé");
        }

        if (endDateTime.isBefore(startDateTime) || endDateTime.isEqual(startDateTime)) {
            throw new BookingValidationException("La date/heure de fin doit être après la date/heure de début");
        }

        long minutesBetween = Duration.between(startDateTime, endDateTime).toMinutes();

        if (minutesBetween < 60) {
            throw new BookingValidationException("La durée minimale de réservation est de 1 heure");
        }

        if (minutesBetween > 10080) {
            throw new BookingValidationException("La durée maximale de réservation est de 7 jours (168 heures)");
        }
    }

    private BigDecimal calculateBookingAmount(BigDecimal hourlyPrice,
                                              LocalDateTime startDateTime,
                                              LocalDateTime endDateTime) {
        // Calculer la durée en heures (arrondi au supérieur)
        long minutes = Duration.between(startDateTime, endDateTime).toMinutes();
        double hours = Math.ceil(minutes / 60.0);

        BigDecimal totalAmount = hourlyPrice
                .multiply(BigDecimal.valueOf(hours))
                .setScale(2, RoundingMode.HALF_UP);

        log.info("Calculated booking amount: {} (hourly rate: {}, duration: {} hours)",
                totalAmount, hourlyPrice, hours);

        return totalAmount;
    }

    private boolean isUserAuthorizedForBooking(Booking booking, User user) {
        if (booking.getUser().getId().equals(user.getId())) {
            return true;
        }

        if (booking.getChargingStation().getUser().getId().equals(user.getId())) {
            return true;
        }

        return false;
    }

    private void verifyStationOwnership(String stationId, User owner) {
        ChargingStation station = stationRepository.findById(stationId)
                .orElseThrow(() -> new BookingValidationException("Borne introuvable"));

        if (!station.getUser().getId().equals(owner.getId())) {
            throw new AccessDeniedException("Vous n'êtes pas propriétaire de cette borne");
        }
    }
}