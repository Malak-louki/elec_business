package com.hb.cda.elec_business.controller;

import com.hb.cda.elec_business.dto.booking.BookingRequestDto;
import com.hb.cda.elec_business.dto.booking.BookingResponseDto;
import com.hb.cda.elec_business.entity.BookingStatus;
import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.exception.BookingConflictException;
import com.hb.cda.elec_business.exception.BookingValidationException;
import com.hb.cda.elec_business.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller pour la gestion des réservations de bornes de recharge
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    /**
     * Créer une nouvelle réservation
     * POST /api/bookings
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingResponseDto> createBooking(
            @Valid @RequestBody BookingRequestDto request,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("User {} creating booking for station {}", currentUser.getEmail(), request.getChargingStationId());

        try {
            BookingResponseDto booking = bookingService.createBooking(request, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(booking);
        } catch (BookingValidationException e) {
            log.warn("Booking validation failed: {}", e.getMessage());
            throw e;
        } catch (BookingConflictException e) {
            log.warn("Booking conflict: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Récupérer une réservation par ID
     * GET /api/bookings/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingResponseDto> getBookingById(
            @PathVariable String id,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("User {} fetching booking {}", currentUser.getEmail(), id);

        try {
            BookingResponseDto booking = bookingService.getBookingById(id, currentUser);
            return ResponseEntity.ok(booking);
        } catch (AccessDeniedException e) {
            log.warn("Access denied for user {} to booking {}", currentUser.getEmail(), id);
            throw e;
        }
    }

    /**
     * Récupérer toutes mes réservations
     * GET /api/bookings/my
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BookingResponseDto>> getMyBookings(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("User {} fetching all their bookings", currentUser.getEmail());

        List<BookingResponseDto> bookings = bookingService.getMyBookings(currentUser);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Récupérer mes réservations par statut
     * GET /api/bookings/my?status=CONFIRMED
     */
    @GetMapping("/my/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BookingResponseDto>> getMyBookingsByStatus(
            @PathVariable BookingStatus status,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("User {} fetching bookings with status {}", currentUser.getEmail(), status);

        List<BookingResponseDto> bookings = bookingService.getMyBookingsByStatus(currentUser, status);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Annuler une réservation
     * DELETE /api/bookings/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> cancelBooking(
            @PathVariable String id,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("User {} cancelling booking {}", currentUser.getEmail(), id);

        try {
            bookingService.cancelBooking(id, currentUser);
            return ResponseEntity.ok(Map.of(
                    "message", "Réservation annulée avec succès",
                    "bookingId", id
            ));
        } catch (BookingValidationException e) {
            log.warn("Cannot cancel booking {}: {}", id, e.getMessage());
            throw e;
        } catch (AccessDeniedException e) {
            log.warn("Access denied for user {} to cancel booking {}", currentUser.getEmail(), id);
            throw e;
        }
    }
    /**
     * Vérifier la disponibilité d'un créneau
     * GET /api/bookings/availability?stationId=xxx&start=2024-01-15T10:00:00&end=2024-01-15T12:00:00
     */
    @GetMapping("/availability")
    public ResponseEntity<Map<String, Boolean>> checkAvailability(
            @RequestParam String stationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        log.info("Checking availability for station {} from {} to {}", stationId, start, end);

        boolean available = bookingService.isSlotAvailable(stationId, start, end);

        return ResponseEntity.ok(Map.of("available", available));
    }

    /**
     * Récupérer les créneaux réservés pour une borne
     * GET /api/bookings/booked-slots?stationId=xxx&start=2024-01-15T00:00:00&end=2024-01-20T23:59:59
     */
    @GetMapping("/booked-slots")
    public ResponseEntity<List<BookingResponseDto>> getBookedSlots(
            @RequestParam String stationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        log.info("Fetching booked slots for station {} from {} to {}", stationId, start, end);

        List<BookingResponseDto> bookedSlots = bookingService.getBookedSlotsForStation(stationId, start, end);

        return ResponseEntity.ok(bookedSlots);
    }

    // ====================================================================
    // ENDPOINTS POUR LES PROPRIÉTAIRES (OWNER DASHBOARD)
    // ====================================================================

    /**
     * Récupérer toutes les réservations pour mes bornes
     * GET /api/bookings/owner/all
     */
    @GetMapping("/owner/all")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<BookingResponseDto>> getAllBookingsForOwner(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("Owner {} fetching all bookings for their stations", currentUser.getEmail());

        List<BookingResponseDto> bookings = bookingService.getAllBookingsForOwner(currentUser);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Récupérer les réservations pour une borne spécifique
     * GET /api/bookings/owner/station/{stationId}
     */
    @GetMapping("/owner/station/{stationId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<BookingResponseDto>> getBookingsForStation(
            @PathVariable String stationId,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("Owner {} fetching bookings for station {}", currentUser.getEmail(), stationId);

        try {
            List<BookingResponseDto> bookings = bookingService.getBookingsForStation(stationId, currentUser);
            return ResponseEntity.ok(bookings);
        } catch (AccessDeniedException e) {
            log.warn("Access denied for user {} to station {}", currentUser.getEmail(), stationId);
            throw e;
        }
    }

    /**
     * Récupérer les réservations à venir pour mes bornes
     * GET /api/bookings/owner/upcoming
     */
    @GetMapping("/owner/upcoming")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<BookingResponseDto>> getUpcomingBookings(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("Owner {} fetching upcoming bookings", currentUser.getEmail());

        List<BookingResponseDto> bookings = bookingService.getUpcomingBookingsForOwner(currentUser);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Récupérer les réservations passées pour mes bornes
     * GET /api/bookings/owner/past
     */
    @GetMapping("/owner/past")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<BookingResponseDto>> getPastBookings(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("Owner {} fetching past bookings", currentUser.getEmail());

        List<BookingResponseDto> bookings = bookingService.getPastBookingsForOwner(currentUser);
        return ResponseEntity.ok(bookings);
    }

    // ====================================================================
    // GESTION DES ERREURS
    // ====================================================================

    @ExceptionHandler(BookingValidationException.class)
    public ResponseEntity<Map<String, String>> handleBookingValidationException(BookingValidationException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "VALIDATION_ERROR",
                "message", e.getMessage()
        ));
    }

    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<Map<String, String>> handleBookingConflictException(BookingConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "BOOKING_CONFLICT",
                "message", e.getMessage()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "ACCESS_DENIED",
                "message", e.getMessage()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "INVALID_ARGUMENT",
                "message", e.getMessage()
        ));
    }
}