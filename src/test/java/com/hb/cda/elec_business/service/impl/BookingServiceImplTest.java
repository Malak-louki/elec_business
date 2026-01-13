package com.hb.cda.elec_business.service.impl;

import com.hb.cda.elec_business.dto.booking.BookingRequestDto;
import com.hb.cda.elec_business.dto.booking.BookingResponseDto;
import com.hb.cda.elec_business.entity.*;
import com.hb.cda.elec_business.exception.BookingConflictException;
import com.hb.cda.elec_business.exception.BookingValidationException;
import com.hb.cda.elec_business.repository.BookingRepository;
import com.hb.cda.elec_business.repository.ChargingStationRepository;
import com.hb.cda.elec_business.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ChargingStationRepository stationRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User testUser;
    private ChargingStation testStation;

    @BeforeEach
    void setUp() {
        // Configuration
        ReflectionTestUtils.setField(bookingService, "timezone", "Europe/Paris");
        ReflectionTestUtils.setField(bookingService, "paymentTimeoutMinutes", 15);
        ReflectionTestUtils.setField(bookingService, "minDurationHours", 1);
        ReflectionTestUtils.setField(bookingService, "maxDurationDays", 7);
        ReflectionTestUtils.setField(bookingService, "maxConcurrentBookings", 5);
        ReflectionTestUtils.setField(bookingService, "cancellationDeadlineHours", 24);

        testUser = new User();
        testUser.setId(UUID.randomUUID().toString());
        testUser.setEmail("test@example.com");

        testStation = new ChargingStation();
        testStation.setId(UUID.randomUUID().toString());
        testStation.setName("Borne Test");
        testStation.setHourlyPrice(new BigDecimal("5.00"));
        testStation.setAvailable(true);
    }

    // ================================================================
    // TEST 1 : DÉTECTION DE CONFLIT
    // ================================================================

    @Test
    @DisplayName("TEST 1 : Doit lever BookingConflictException si créneau déjà pris")
    void createBooking_shouldThrowConflictException_whenSlotAlreadyBooked() {
        // GIVEN
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        BookingRequestDto request = BookingRequestDto.builder()
                .chargingStationId(testStation.getId())
                .startDateTime(start)
                .endDateTime(end)
                .build();

        when(stationRepository.findByIdWithRelations(testStation.getId()))
                .thenReturn(Optional.of(testStation));
        when(bookingRepository.countActiveBookingsByUser(testUser)).thenReturn(0L);

        // SIMULATION D'UN CONFLIT
        when(bookingRepository.existsConflictingBooking(eq(testStation), any(), any()))
                .thenReturn(true);

        // WHEN & THEN
        BookingConflictException exception = assertThrows(
                BookingConflictException.class,
                () -> bookingService.createBooking(request, testUser)
        );

        assertTrue(exception.getMessage().contains("déjà réservé"));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    // ================================================================
    // TEST 2 : CALCUL PRIX EXACT (2 heures exactes = 10€)
    // ================================================================

    @Test
    @DisplayName("TEST 2 : 2 heures exactes → 2h × 5€ = 10€")
    void createBooking_shouldCalculateCorrectAmount_forExactHours() {
        // GIVEN - 2 heures exactes
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        LocalDateTime end = start.plusHours(2); // 16h00

        BookingRequestDto request = BookingRequestDto.builder()
                .chargingStationId(testStation.getId())
                .startDateTime(start)
                .endDateTime(end)
                .build();

        setupMocksForSuccess();

        // WHEN
        BookingResponseDto response = bookingService.createBooking(request, testUser);

        // THEN
        assertEquals(new BigDecimal("10.00"), response.getTotalAmount(),
                "2h × 5€ = 10€");
    }

    // ================================================================
    // TEST 3 : CALCUL PRIX ARRONDI (2h30 → 3h = 15€)
    // ================================================================

    @Test
    @DisplayName("TEST 3 : 2h30 arrondi à 3h → 3h × 5€ = 15€")
    void createBooking_shouldRoundUp_for2h30() {
        // GIVEN - 2h30
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        LocalDateTime end = start.plusHours(2).plusMinutes(30); // 16h30

        BookingRequestDto request = BookingRequestDto.builder()
                .chargingStationId(testStation.getId())
                .startDateTime(start)
                .endDateTime(end)
                .build();

        setupMocksForSuccess();

        // WHEN
        BookingResponseDto response = bookingService.createBooking(request, testUser);

        // THEN
        assertEquals(new BigDecimal("15.00"), response.getTotalAmount(),
                "2h30 arrondi à 3h × 5€ = 15€");
    }

    // ================================================================
    // TEST 4 : CALCUL PRIX ARRONDI MINIMAL (1h01 → 2h = 10€)
    // ================================================================

    @Test
    @DisplayName("TEST 4 : 1h01 arrondi à 2h → 2h × 5€ = 10€")
    void createBooking_shouldRoundUp_for1h01() {
        // GIVEN - 1h01
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        LocalDateTime end = start.plusHours(1).plusMinutes(1); // 15h01

        BookingRequestDto request = BookingRequestDto.builder()
                .chargingStationId(testStation.getId())
                .startDateTime(start)
                .endDateTime(end)
                .build();

        setupMocksForSuccess();

        // WHEN
        BookingResponseDto response = bookingService.createBooking(request, testUser);

        // THEN
        assertEquals(new BigDecimal("10.00"), response.getTotalAmount(),
                "1h01 arrondi à 2h × 5€ = 10€");
    }

    // ================================================================
    // TEST 5 : VALIDATION DURÉE MINIMALE
    // ================================================================

    @Test
    @DisplayName("TEST 5 : Doit refuser une réservation < 1h (durée minimale)")
    void createBooking_shouldReject_ifDurationTooShort() {
        // GIVEN - 30 minutes (< 1h minimum)
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        LocalDateTime end = start.plusMinutes(30);

        BookingRequestDto request = BookingRequestDto.builder()
                .chargingStationId(testStation.getId())
                .startDateTime(start)
                .endDateTime(end)
                .build();

        // WHEN & THEN
        BookingValidationException exception = assertThrows(
                BookingValidationException.class,
                () -> bookingService.createBooking(request, testUser)
        );

        assertTrue(exception.getMessage().contains("Durée minimale"));
    }

    // ================================================================
    // TEST 6 : VALIDATION DURÉE MAXIMALE
    // ================================================================

    @Test
    @DisplayName("TEST 6 : Doit refuser une réservation > 7 jours (CORRECTION toHours)")
    void createBooking_shouldReject_ifDurationTooLong() {
        // GIVEN - 7 jours + 1 heure (> 7 jours maximum)
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        LocalDateTime end = start.plusDays(7).plusHours(1); // 169 heures > 168h (7j)

        BookingRequestDto request = BookingRequestDto.builder()
                .chargingStationId(testStation.getId())
                .startDateTime(start)
                .endDateTime(end)
                .build();

        // WHEN & THEN
        BookingValidationException exception = assertThrows(
                BookingValidationException.class,
                () -> bookingService.createBooking(request, testUser)
        );

        assertTrue(exception.getMessage().contains("Durée maximale"));
        assertTrue(exception.getMessage().contains("168 heures")); // 7j × 24h
    }

    // ================================================================
    // TEST 7 : EXPIRATION AUTOMATIQUE
    // ================================================================
    @Test
    @DisplayName("TEST 7 : Doit calculer expiresAt = maintenant + 15 minutes")
    void createBooking_shouldSetExpiresAt_correctly() {
        // GIVEN
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        BookingRequestDto request = BookingRequestDto.builder()
                .chargingStationId(testStation.getId())
                .startDateTime(start)
                .endDateTime(end)
                .build();

        // Configuration des mocks
        when(stationRepository.findByIdWithRelations(testStation.getId()))
                .thenReturn(Optional.of(testStation));
        when(bookingRepository.existsConflictingBooking(any(), any(), any()))
                .thenReturn(false);
        when(bookingRepository.countActiveBookingsByUser(testUser)).thenReturn(0L);

        // CORRECTION : Capturer le booking AVANT le save pour vérifier expiresAt
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        when(bookingRepository.save(bookingCaptor.capture())).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(UUID.randomUUID().toString());
            // On retourne le booking avec son expiresAt déjà défini
            return b;
        });

        // Capturer l'instant AVANT l'appel
        Instant beforeCall = Instant.now();

        // WHEN
        bookingService.createBooking(request, testUser);

        // THEN
        Booking savedBooking = bookingCaptor.getValue();

        // Vérification 1 : expiresAt existe
        assertNotNull(savedBooking.getExpiresAt(),
                "expiresAt doit être défini");

        // Vérification 2 : expiresAt est environ maintenant + 15 minutes
        Instant expectedExpiry = beforeCall.plus(15, ChronoUnit.MINUTES);

        long diffSeconds = Math.abs(ChronoUnit.SECONDS.between(
                savedBooking.getExpiresAt(),
                expectedExpiry
        ));

        // CORRECTION : Tolérance augmentée à 10 secondes pour les tests lents
        assertTrue(diffSeconds < 10,
                String.format(
                        "expiresAt doit être environ maintenant + 15 minutes. " +
                                "Différence constatée : %d secondes (tolérance : 10s)",
                        diffSeconds
                ));
    }
    // ================================================================
    // HELPER
    // ================================================================

    private void setupMocksForSuccess() {
        when(stationRepository.findByIdWithRelations(testStation.getId()))
                .thenReturn(Optional.of(testStation));
        when(bookingRepository.existsConflictingBooking(any(), any(), any()))
                .thenReturn(false);
        when(bookingRepository.countActiveBookingsByUser(testUser)).thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking b = i.getArgument(0);
            b.setId(UUID.randomUUID().toString());
            return b;
        });
    }
}
