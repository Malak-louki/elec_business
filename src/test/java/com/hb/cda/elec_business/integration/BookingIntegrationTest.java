package com.hb.cda.elec_business.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hb.cda.elec_business.dto.booking.BookingRequestDto;
import com.hb.cda.elec_business.entity.*;
import com.hb.cda.elec_business.repository.*;
import com.hb.cda.elec_business.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TEST D'INTÉGRATION 1 : Flux complet de création de réservation
 *
 * Ce test valide le parcours complet :
 * Controller → Service → Repository → Base de données H2
 *
 * Objectif CDA : Démontrer la maîtrise des tests d'intégration
 * couvrant plusieurs couches applicatives.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ChargingStationRepository stationRepository;

    @Autowired
    private ChargingLocationRepository locationRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private ChargingStation testStation;
    private String validToken;

    @BeforeEach
    void setUp() {
        // Nettoyage complet
        bookingRepository.deleteAll();
        stationRepository.deleteAll();
        locationRepository.deleteAll();
        addressRepository.deleteAll();
        userRepository.deleteAll();

        // ========== CRÉATION DU RÔLE USER ==========
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.USER);
                    return roleRepository.save(role);
                });

        // ========== CRÉATION D'UN UTILISATEUR ACTIVÉ ==========
        testUser = new User();
        testUser.setEmail("client@test.com");
        testUser.setFirstName("Jean");
        testUser.setLastName("Dupont");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setUserStatus(UserStatus.ACTIVE);
        testUser.setRoles(Set.of(userRole));
        testUser = userRepository.save(testUser);

        // Génération du token JWT
        validToken = jwtService.generateAccessToken(testUser);

        // ========== CRÉATION D'UNE ADRESSE ==========
        Address address = new Address();
        address.setStreet("Rue de la Paix");
        address.setNumber("10");
        address.setCity("Paris");
        address.setPostalCode("75001");
        address.setCountry("France");
        address = addressRepository.save(address);

        // ========== CRÉATION D'UNE CHARGING LOCATION ==========
        ChargingLocation location = new ChargingLocation();
        location.setLatitude(new BigDecimal("48.8566"));
        location.setLongitude(new BigDecimal("2.3522"));
        location.setAvailable(true);
        location.setAddress(address);
        location = locationRepository.save(location);

        // ========== CRÉATION D'UNE STATION DE RECHARGE ==========
        testStation = new ChargingStation();
        testStation.setName("Borne Test Paris");
        testStation.setHourlyPrice(new BigDecimal("5.00"));
        testStation.setChargingPowerKw(new BigDecimal("22.00"));
        testStation.setChargingPower("22 kW");
        testStation.setAvailable(true);
        testStation.setHasStand(true);
        testStation.setChargingLocation(location);
        testStation.setUser(testUser); // Propriétaire pour simplifier
        testStation = stationRepository.save(testStation);
    }

    @Test
    @DisplayName("TEST INTÉGRATION 1 : Création réussie d'une réservation - Flux complet")
    void shouldCreateBookingSuccessfully_fullFlow() throws Exception {
        // ========== GIVEN ==========
        // Créneau disponible dans 2 jours, durée 3 heures
        LocalDateTime start = LocalDateTime.now().plusDays(2)
                .withHour(14).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(3); // 3h × 5€ = 15€

        BookingRequestDto request = BookingRequestDto.builder()
                .chargingStationId(testStation.getId())
                .startDateTime(start)
                .endDateTime(end)
                .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // ========== WHEN ==========
        // Appel HTTP POST vers /api/bookings
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + validToken)
                        .content(requestJson))
                // ========== THEN - Vérification HTTP ==========
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.bookingStatus").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(15.00))
                .andExpect(jsonPath("$.startDateTime").exists())
                .andExpect(jsonPath("$.endDateTime").exists());

        // ========== THEN - Vérification en base de données ==========
        assertThat(bookingRepository.count()).isEqualTo(1);

        Booking savedBooking = bookingRepository.findAll().get(0);
        assertThat(savedBooking.getChargingStation().getId()).isEqualTo(testStation.getId());
        assertThat(savedBooking.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(savedBooking.getBookingStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(savedBooking.getTotalAmount()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(savedBooking.getExpiresAt()).isNotNull(); // Expire dans 15 minutes
    }

    @Test
    @DisplayName("TEST INTÉGRATION 2 : Refus si créneau déjà réservé (conflit)")
    void shouldRejectConflictingBooking() throws Exception {
        // ========== GIVEN ==========
        // Première réservation existante (CONFIRMED)
        LocalDateTime start = LocalDateTime.now().plusDays(3)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);

        Booking existingBooking = new Booking();
        existingBooking.setUser(testUser);
        existingBooking.setChargingStation(testStation);
        existingBooking.setStartDateTime(start);
        existingBooking.setEndDateTime(end);
        existingBooking.setBookingStatus(BookingStatus.CONFIRMED);
        existingBooking.setTotalAmount(new BigDecimal("10.00"));
        bookingRepository.save(existingBooking);

        // ========== WHEN ==========
        // Tentative de réservation qui chevauche (commence 30 min après)
        LocalDateTime conflictStart = start.plusMinutes(30);
        LocalDateTime conflictEnd = conflictStart.plusHours(2);

        BookingRequestDto conflictRequest = BookingRequestDto.builder()
                .chargingStationId(testStation.getId())
                .startDateTime(conflictStart)
                .endDateTime(conflictEnd)
                .build();

        String requestJson = objectMapper.writeValueAsString(conflictRequest);

        // ========== THEN ==========
        // Doit retourner 409 CONFLICT
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + validToken)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());

        // Vérification : une seule réservation en base (la première)
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("TEST INTÉGRATION 3 : Refus si station inexistante (404)")
    void shouldRejectBooking_whenStationDoesNotExist() throws Exception {
        // ========== GIVEN ==========
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        BookingRequestDto request = BookingRequestDto.builder()
                .chargingStationId("station-inexistante-uuid-12345")
                .startDateTime(start)
                .endDateTime(end)
                .build();

        String requestJson = objectMapper.writeValueAsString(request);

        // ========== WHEN & THEN ==========
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + validToken)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        // Aucune réservation créée
        assertThat(bookingRepository.count()).isZero();
    }
}