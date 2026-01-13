package com.hb.cda.elec_business.controller;

import com.hb.cda.elec_business.dto.booking.BookingRequestDto;
import com.hb.cda.elec_business.dto.booking.BookingResponseDto;
import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.service.BookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contrôleur REST pour les réservations - VERSION FINALE CORRIGÉE
 *
 * CORRECTIONS APPLIQUÉES :
 * - Messages d'erreur en français
 * - Rate limiting simple (ConcurrentHashMap)
 * - Utilisation de totalAmount (cohérent avec l'Entity)
 *
 * @author Votre Nom - CDA 2026
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    // ====================================================================
    // RATE LIMITING SIMPLE (niveau CDA)
    // ====================================================================

    /**
     * Map pour stocker le nombre de requêtes par IP
     * Structure : IP → Map(minute → nombre de requêtes)
     */
    private final ConcurrentHashMap<String, Map<String, Integer>> rateLimitMap = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    /**
     * Vérifie si l'IP a dépassé la limite de requêtes
     */
    private boolean isRateLimited(String ip) {
        String currentMinute = getCurrentMinute();

        rateLimitMap.putIfAbsent(ip, new ConcurrentHashMap<>());
        Map<String, Integer> requestsByMinute = rateLimitMap.get(ip);

        int count = requestsByMinute.getOrDefault(currentMinute, 0);

        if (count >= MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit dépassé pour IP: {}", ip);
            return true;
        }

        requestsByMinute.put(currentMinute, count + 1);

        // Nettoyer les anciennes minutes (garder seulement les 5 dernières)
        if (requestsByMinute.size() > 5) {
            requestsByMinute.keySet().stream()
                    .sorted()
                    .limit(requestsByMinute.size() - 5)
                    .forEach(requestsByMinute::remove);
        }

        return false;
    }

    private String getCurrentMinute() {
        return LocalDateTime.now().toString().substring(0, 16); // "2026-01-15T14:30"
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isEmpty()) ? ip.split(",")[0] : request.getRemoteAddr();
    }

    // ====================================================================
    // ENDPOINTS - GESTION DES RÉSERVATIONS
    // ====================================================================

    /**
     * Créer une nouvelle réservation
     * POST /api/bookings
     *
     * Body JSON :
     * {
     *   "chargingStationId": "uuid-de-la-borne",
     *   "startDateTime": "2026-01-15T14:00:00",
     *   "endDateTime": "2026-01-15T16:00:00"
     * }
     *
     * Nécessite : JWT valide (utilisateur connecté)
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingResponseDto> createBooking(
            @Valid @RequestBody BookingRequestDto request,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();

        log.info("Création réservation pour {} sur borne {}",
                currentUser.getEmail(), request.getChargingStationId());

        BookingResponseDto response = bookingService.createBooking(request, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Annuler une réservation
     * PUT /api/bookings/{id}/cancel
     *
     * Nécessite : JWT valide + être le propriétaire de la réservation
     * Contrainte : Au moins 24h avant le début (configurable)
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingResponseDto> cancelBooking(
            @PathVariable String id,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("Annulation réservation {} par {}", id, currentUser.getEmail());

        BookingResponseDto response = bookingService.cancelBooking(id, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Confirmer une réservation après paiement
     * PUT /api/bookings/{id}/confirm
     *
     * Nécessite : Rôle ADMIN (appelé normalement par webhook Stripe)
     */
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookingResponseDto> confirmBooking(
            @PathVariable String id,
            @RequestParam String paymentId
    ) {
        log.info("Confirmation réservation {} avec paiement {}", id, paymentId);
        BookingResponseDto response = bookingService.confirmBooking(id, paymentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Marquer une réservation comme terminée
     * PUT /api/bookings/{id}/complete
     *
     * Nécessite : Rôle ADMIN
     * Note : Normalement fait automatiquement par le scheduler
     */
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BookingResponseDto> completeBooking(@PathVariable String id) {
        log.info("Finalisation manuelle réservation {}", id);
        BookingResponseDto response = bookingService.completeBooking(id);
        return ResponseEntity.ok(response);
    }

    // ====================================================================
    // ENDPOINTS - CONSULTATION
    // ====================================================================

    /**
     * Récupérer les détails d'une réservation
     * GET /api/bookings/{id}
     *
     * Accessible par :
     * - L'utilisateur qui a fait la réservation
     * - Le propriétaire de la borne
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingResponseDto> getBookingById(
            @PathVariable String id,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        BookingResponseDto response = bookingService.getBookingById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Récupérer toutes mes réservations
     * GET /api/bookings/mine
     *
     * Retourne toutes les réservations de l'utilisateur connecté
     * (tous statuts confondus)
     */
    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BookingResponseDto>> getMyBookings(
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("Récupération des réservations de {}", currentUser.getEmail());

        List<BookingResponseDto> bookings = bookingService.getMyBookings(currentUser);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Récupérer mes réservations par statut
     * GET /api/bookings/mine/status/{status}
     *
     * Statuts possibles : PENDING, CONFIRMED, COMPLETED, CANCELLED, EXPIRED
     */
    @GetMapping("/mine/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BookingResponseDto>> getMyBookingsByStatus(
            @PathVariable String status,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("Récupération des réservations {} de {}", status, currentUser.getEmail());

        List<BookingResponseDto> bookings = bookingService.getMyBookingsByStatus(currentUser, status);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Récupérer mes réservations à venir
     * GET /api/bookings/mine/upcoming
     *
     * Retourne les réservations PENDING ou CONFIRMED dont la date de début
     * est dans le futur
     */
    @GetMapping("/mine/upcoming")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BookingResponseDto>> getMyUpcomingBookings(
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("Récupération des réservations à venir de {}", currentUser.getEmail());

        List<BookingResponseDto> bookings = bookingService.getMyUpcomingBookings(currentUser);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Récupérer mes réservations passées
     * GET /api/bookings/mine/past
     *
     * Retourne les réservations dont la date de fin est passée
     */
    @GetMapping("/mine/past")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BookingResponseDto>> getMyPastBookings(
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("Récupération de l'historique de {}", currentUser.getEmail());

        List<BookingResponseDto> bookings = bookingService.getMyPastBookings(currentUser);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Récupérer les réservations d'une de mes bornes (propriétaire)
     * GET /api/bookings/station/{chargingStationId}
     *
     * Accessible uniquement par le propriétaire de la borne
     */
    @GetMapping("/station/{chargingStationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BookingResponseDto>> getBookingsByStation(
            @PathVariable String chargingStationId,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("Récupération des réservations de la borne {} par {}",
                chargingStationId, currentUser.getEmail());

        List<BookingResponseDto> bookings = bookingService.getBookingsByStation(
                chargingStationId, currentUser);
        return ResponseEntity.ok(bookings);
    }

    // ====================================================================
    // ENDPOINT PUBLIC - CHECK AVAILABILITY (avec rate limiting)
    // ====================================================================

    /**
     * Vérifier la disponibilité d'une borne
     * GET /api/bookings/check-availability
     *
     * ENDPOINT PUBLIC (pas de JWT requis) avec RATE LIMITING
     * Limite : 10 requêtes par minute par IP
     *
     * Paramètres :
     * - chargingStationId : UUID de la borne
     * - startDateTime : 2026-01-15T14:00:00
     * - endDateTime : 2026-01-15T16:00:00
     *
     * Retourne : { "available": true/false }
     */
    @GetMapping("/check-availability")
    public ResponseEntity<?> checkAvailability(
            @RequestParam String chargingStationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDateTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDateTime,
            HttpServletRequest request
    ) {
        // RATE LIMITING
        String clientIp = getClientIp(request);

        if (isRateLimited(clientIp)) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Trop de requêtes");
            error.put("message", "Veuillez patienter avant de vérifier à nouveau la disponibilité");
            error.put("retryAfter", "60 secondes");

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
        }

        log.debug("Vérification disponibilité borne {} de {} à {} (IP: {})",
                chargingStationId, startDateTime, endDateTime, clientIp);

        boolean available = bookingService.checkAvailability(
                chargingStationId, startDateTime, endDateTime);

        Map<String, Object> response = new HashMap<>();
        response.put("available", available);
        response.put("chargingStationId", chargingStationId);
        response.put("startDateTime", startDateTime);
        response.put("endDateTime", endDateTime);

        return ResponseEntity.ok(response);
    }
}