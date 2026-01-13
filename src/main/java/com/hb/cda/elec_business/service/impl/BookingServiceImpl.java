package com.hb.cda.elec_business.service.impl;

import com.hb.cda.elec_business.dto.booking.BookingRequestDto;
import com.hb.cda.elec_business.dto.booking.BookingResponseDto;
import com.hb.cda.elec_business.entity.*;
import com.hb.cda.elec_business.exception.BookingConflictException;
import com.hb.cda.elec_business.exception.BookingValidationException;
import com.hb.cda.elec_business.mapper.BookingMapper;
import com.hb.cda.elec_business.repository.BookingRepository;
import com.hb.cda.elec_business.repository.ChargingStationRepository;
import com.hb.cda.elec_business.repository.PaymentRepository;
import com.hb.cda.elec_business.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ChargingStationRepository stationRepository;
    private final PaymentRepository paymentRepository;

    // Timezone centralisée
    @Value("${app.timezone:Europe/Paris}")
    private String timezone;

    @Value("${app.booking.payment.timeout-minutes:15}")
    private int paymentTimeoutMinutes;

    @Value("${app.booking.min-duration-hours:1}")
    private int minDurationHours;

    @Value("${app.booking.max-duration-days:7}")
    private int maxDurationDays;

    @Value("${app.booking.max-concurrent-bookings:5}")
    private int maxConcurrentBookings;

    @Value("${app.booking.cancellation-deadline-hours:24}")
    private int cancellationDeadlineHours;

    /**
     * Crée une nouvelle réservation
     */
    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto request, User user) {
        log.info("Création réservation pour {} sur borne {}",
                user.getEmail(), request.getChargingStationId());

        // ÉTAPE 1 : Validations de base
        validateBookingRequest(request);
        checkUserLimits(user);

        // ÉTAPE 2 : Récupération de la borne
        ChargingStation station = stationRepository
                .findByIdWithRelations(request.getChargingStationId())
                .orElseThrow(() -> new BookingValidationException(
                        "Borne non trouvée : " + request.getChargingStationId()));

        if (!station.isAvailable()) {
            throw new BookingValidationException("Cette borne n'est pas disponible à la réservation");
        }

        // ÉTAPE 3 : Vérification SYNCHRONISÉE des conflits
        checkConflictAndReserve(station, request.getStartDateTime(), request.getEndDateTime());

        // ÉTAPE 4 : Calcul du montant total
        BigDecimal totalAmount = calculateTotalAmount(
                station.getHourlyPrice(),
                request.getStartDateTime(),
                request.getEndDateTime()
        );

        // ÉTAPE 5 : Création de la réservation
        Booking booking = new Booking();
        booking.setStartDateTime(request.getStartDateTime());
        booking.setEndDateTime(request.getEndDateTime());
        booking.setTotalAmount(totalAmount); // CORRECTION : totalAmount
        booking.setBookingStatus(BookingStatus.PENDING);
        booking.setUser(user);
        booking.setChargingStation(station);
        booking.setExpiresAt(Instant.now().plus(paymentTimeoutMinutes, ChronoUnit.MINUTES));

        booking = bookingRepository.save(booking);

        log.info("Réservation créée : ID={}, Montant={}€, Expire à {}",
                booking.getId(), totalAmount, booking.getExpiresAt());

        return BookingMapper.toResponseDto(booking);
    }

    /**
     * Méthode SYNCHRONISÉE pour vérifier les conflits
     */
    private synchronized void checkConflictAndReserve(
            ChargingStation station,
            LocalDateTime start,
            LocalDateTime end) {

        boolean hasConflict = bookingRepository.existsConflictingBooking(station, start, end);

        if (hasConflict) {
            log.warn("Conflit détecté pour borne {} entre {} et {}",
                    station.getId(), start, end);
            throw new BookingConflictException(
                    "Ce créneau est déjà réservé. Veuillez choisir une autre période.");
        }
    }

    /**
     * Annule une réservation
     */
    @Override
    @Transactional
    public BookingResponseDto cancelBooking(String bookingId, User user) {
        log.info("Annulation réservation {} par {}", bookingId, user.getEmail());

        Booking booking = bookingRepository.findByIdWithRelations(bookingId)
                .orElseThrow(() -> new BookingValidationException("Réservation non trouvée"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Vous ne pouvez pas annuler cette réservation");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED ||
                booking.getBookingStatus() == BookingStatus.COMPLETED ||
                booking.getBookingStatus() == BookingStatus.EXPIRED) {
            throw new BookingValidationException(
                    "Impossible d'annuler une réservation avec le statut : " +
                            booking.getBookingStatus());
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of(timezone));
        long hoursUntilStart = Duration.between(now, booking.getStartDateTime()).toHours();

        if (hoursUntilStart < cancellationDeadlineHours) {
            throw new BookingValidationException(String.format(
                    "Impossible d'annuler moins de %d heures avant le début. Il reste %d heures.",
                    cancellationDeadlineHours, hoursUntilStart
            ));
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);

        log.info("Réservation {} annulée avec succès", bookingId);
        return BookingMapper.toResponseDto(booking);
    }

    /**
     * Confirme une réservation après paiement
     */
    @Override
    @Transactional
    public BookingResponseDto confirmBooking(String bookingId, String paymentId) {
        log.info("Confirmation réservation {} avec paiement {}", bookingId, paymentId);

        Booking booking = bookingRepository.findByIdWithRelations(bookingId)
                .orElseThrow(() -> new BookingValidationException("Réservation non trouvée"));

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new BookingValidationException(
                    "Impossible de confirmer une réservation avec le statut : " +
                            booking.getBookingStatus());
        }

        if (booking.getExpiresAt() != null && Instant.now().isAfter(booking.getExpiresAt())) {
            booking.setBookingStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            throw new BookingValidationException(
                    "Réservation expirée. Le délai de paiement a été dépassé.");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BookingValidationException("Paiement non trouvé"));

        if (payment.getPaymentStatus() != PaymentStatus.SUCCEEDED) {
            throw new BookingValidationException(
                    "Impossible de confirmer avec un paiement au statut : " +
                            payment.getPaymentStatus());
        }

        booking.setPayment(payment);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);

        log.info("Réservation {} confirmée avec succès", bookingId);
        return BookingMapper.toResponseDto(booking);
    }

    /**
     * Marque une réservation comme terminée
     */
    @Override
    @Transactional
    public BookingResponseDto completeBooking(String bookingId) {
        log.info("Finalisation manuelle réservation {}", bookingId);

        Booking booking = bookingRepository.findByIdWithRelations(bookingId)
                .orElseThrow(() -> new BookingValidationException("Réservation non trouvée"));

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new BookingValidationException(
                    "Impossible de finaliser une réservation non confirmée");
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of(timezone));
        if (now.isBefore(booking.getEndDateTime())) {
            throw new BookingValidationException(
                    "Impossible de finaliser une réservation qui n'est pas encore terminée");
        }

        booking.setBookingStatus(BookingStatus.COMPLETED);
        booking = bookingRepository.save(booking);

        log.info("Réservation {} finalisée avec succès", bookingId);
        return BookingMapper.toResponseDto(booking);
    }

    // ====================================================================
    // MÉTHODES DE CONSULTATION
    // ====================================================================

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDto getBookingById(String bookingId, User user) {
        Booking booking = bookingRepository.findByIdWithRelations(bookingId)
                .orElseThrow(() -> new BookingValidationException("Réservation non trouvée"));

        if (!booking.getUser().getId().equals(user.getId()) &&
                !booking.getChargingStation().getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Vous ne pouvez pas consulter cette réservation");
        }

        return BookingMapper.toResponseDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getMyBookings(User user) {
        List<Booking> bookings = bookingRepository.findByUserWithRelations(user);
        return BookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getMyBookingsByStatus(User user, String status) {
        BookingStatus bookingStatus;
        try {
            bookingStatus = BookingStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BookingValidationException(
                    "Statut invalide : " + status +
                            ". Valeurs possibles : PENDING, CONFIRMED, COMPLETED, CANCELLED, EXPIRED");
        }

        List<Booking> bookings = bookingRepository
                .findByUserAndBookingStatusOrderByStartDateTimeDesc(user, bookingStatus);
        return BookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getMyUpcomingBookings(User user) {
        // CORRECTION : Utilisation de la timezone centralisée
        LocalDateTime now = LocalDateTime.now(ZoneId.of(timezone));
        List<Booking> bookings = bookingRepository.findUpcomingBookingsByUser(user, now);
        return BookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getMyPastBookings(User user) {
        // CORRECTION : Utilisation de la timezone centralisée
        LocalDateTime now = LocalDateTime.now(ZoneId.of(timezone));
        List<Booking> bookings = bookingRepository.findPastBookingsByUser(user, now);
        return BookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getBookingsByStation(String stationId, User owner) {
        ChargingStation station = stationRepository.findByIdWithRelations(stationId)
                .orElseThrow(() -> new BookingValidationException("Borne non trouvée"));

        if (!station.getUser().getId().equals(owner.getId())) {
            throw new AccessDeniedException(
                    "Vous ne pouvez pas consulter les réservations de cette borne");
        }

        List<Booking> bookings = bookingRepository.findByChargingStationWithRelations(station);
        return BookingMapper.toResponseDtoList(bookings);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkAvailability(
            String stationId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime) {

        ChargingStation station = stationRepository.findById(stationId)
                .orElseThrow(() -> new BookingValidationException("Borne non trouvée"));

        return !bookingRepository.existsConflictingBooking(station, startDateTime, endDateTime);
    }

    // ====================================================================
    // MÉTHODES PRIVÉES DE VALIDATION ET CALCUL
    // ====================================================================

    /**
     * Valide une demande de réservation
     */
    private void validateBookingRequest(BookingRequestDto request) {

        LocalDateTime now = LocalDateTime.now(ZoneId.of(timezone));

        if (request.getStartDateTime().isBefore(now)) {
            throw new BookingValidationException("La date de début doit être dans le futur");
        }

        if (request.getEndDateTime().isBefore(request.getStartDateTime()) ||
                request.getEndDateTime().equals(request.getStartDateTime())) {
            throw new BookingValidationException(
                    "La date de fin doit être après la date de début");
        }

        // Durée minimale
        Duration duration = Duration.between(request.getStartDateTime(), request.getEndDateTime());
        long hours = duration.toHours();

        if (hours < minDurationHours) {
            throw new BookingValidationException(
                    "Durée minimale : " + minDurationHours + " heure(s)");
        }

        // CORRECTION : Durée maximale en HEURES (pas en jours tronqués)
        long maxHours = maxDurationDays * 24L;
        if (hours > maxHours) {
            throw new BookingValidationException(String.format(
                    "Durée maximale : %d jour(s) (%d heures). Durée demandée : %d heures",
                    maxDurationDays, maxHours, hours
            ));
        }
    }

    /**
     * Vérifie les limites anti-abus
     */
    private void checkUserLimits(User user) {
        if (maxConcurrentBookings > 0) {
            long activeCount = bookingRepository.countActiveBookingsByUser(user);
            if (activeCount >= maxConcurrentBookings) {
                throw new BookingValidationException(String.format(
                        "Vous avez atteint la limite de %d réservations actives. " +
                                "Veuillez annuler ou attendre qu'une réservation se termine.",
                        maxConcurrentBookings
                ));
            }
        }
    }

    /**
     * Calcule le montant total d'une réservation
     * Règle : Toute heure commencée est due (arrondi supérieur)
     */
    private BigDecimal calculateTotalAmount(
            BigDecimal hourlyPrice,
            LocalDateTime start,
            LocalDateTime end) {

        Duration duration = Duration.between(start, end);
        long hours = duration.toHours();
        long remainingMinutes = duration.toMinutes() % 60;

        if (remainingMinutes > 0) {
            hours++;
        }

        BigDecimal amount = hourlyPrice.multiply(BigDecimal.valueOf(hours));

        log.debug("Calcul montant : {}h à {}€/h = {}€", hours, hourlyPrice, amount);

        return amount;
    }
}
