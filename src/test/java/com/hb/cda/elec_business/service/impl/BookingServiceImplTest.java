package com.hb.cda.elec_business.service.impl;

import com.hb.cda.elec_business.dto.booking.BookingRequestDto;
import com.hb.cda.elec_business.dto.booking.BookingResponseDto;
import com.hb.cda.elec_business.entity.*;
import com.hb.cda.elec_business.exception.BookingConflictException;
import com.hb.cda.elec_business.exception.BookingValidationException;
import com.hb.cda.elec_business.repository.BookingRepository;
import com.hb.cda.elec_business.repository.ChargingStationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ChargingStationRepository stationRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User testUser;
    private ChargingStation testStation;

    @BeforeEach
    void setUp() {
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
        when(bookingRepository.existsConflictingBooking(eq(testStation), eq(start), eq(end)))
                .thenReturn(true);

        // WHEN & THEN
        BookingConflictException exception = assertThrows(
                BookingConflictException.class,
                () -> bookingService.createBooking(request, testUser)
        );

        assertTrue(exception.getMessage().contains("créneau"));
    }

    // ================================================================
    // TEST 2 : CALCUL DU MONTANT (HEURES EXACTES)
    // ================================================================

    @Test
    @DisplayName("TEST 2 : Doit calculer correctement le montant pour des heures exactes")
    void createBooking_shouldCalculateCorrectAmount_forExactHours() {
        // GIVEN - 3 heures exactement × 5€/h = 15€
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        LocalDateTime end = start.plusHours(3);

        BookingRequestDto request = BookingRequestDto.builder()
                .chargingStationId(testStation.getId())
                .startDateTime(start)
                .endDateTime(end)
                .build();

        setupMocksForSuccess();

        // WHEN
        BookingResponseDto response = bookingService.createBooking(request, testUser);

        // THEN
        assertEquals(new BigDecimal("15.00"), response.getTotalAmount());
        assertEquals(BookingStatus.PENDING, response.getBookingStatus());
    }

    // ================================================================
    // TEST 3 : EXPIRATION AUTOMATIQUE
    // ================================================================
    @Test
    @DisplayName("TEST 5 : Doit calculer expiresAt = maintenant + 15 minutes")
    void createBooking_shouldSetExpiresAt_correctly() {
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
        when(bookingRepository.existsConflictingBooking(any(ChargingStation.class), any(), any()))
                .thenReturn(false);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        when(bookingRepository.save(bookingCaptor.capture())).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(UUID.randomUUID().toString());
            return b;
        });

        Instant beforeCall = Instant.now();

        // WHEN
        bookingService.createBooking(request, testUser);

        // THEN
        Booking savedBooking = bookingCaptor.getValue();

        assertNotNull(savedBooking.getExpiresAt(), "expiresAt doit être défini");

        Instant expectedExpiry = beforeCall.plus(15, ChronoUnit.MINUTES);
        long diffSeconds = Math.abs(ChronoUnit.SECONDS.between(
                savedBooking.getExpiresAt(),
                expectedExpiry
        ));

        assertTrue(diffSeconds < 10,
                String.format("expiresAt doit être environ maintenant + 15 minutes. " +
                        "Différence constatée : %d secondes (tolérance : 10s)", diffSeconds
                ));
    }

    // ================================================================
    // HELPER
    // ================================================================

    private void setupMocksForSuccess() {
        when(stationRepository.findByIdWithRelations(testStation.getId()))
                .thenReturn(Optional.of(testStation));
        when(bookingRepository.existsConflictingBooking(any(ChargingStation.class), any(), any()))
                .thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking b = i.getArgument(0);
            b.setId(UUID.randomUUID().toString());
            return b;
        });
    }
}