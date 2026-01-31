package com.hb.cda.elec_business.integration;

import com.hb.cda.elec_business.entity.*;
import com.hb.cda.elec_business.repository.*;
import com.hb.cda.elec_business.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TEST D'INTÉGRATION : Dashboard propriétaire
 *
 * Aligné sur OwnerDashboardController :
 * - GET /api/owner/dashboard/stats
 *
 * Objectif CDA : démontrer le RBAC + cohérence des métriques.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardOwnerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ChargingStationRepository stationRepository;
    @Autowired private ChargingLocationRepository locationRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private PaymentRepository paymentRepository;

    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    private User ownerUser;
    private ChargingStation ownerStation;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        // Nettoyage complet (ordre important)
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        stationRepository.deleteAll();
        locationRepository.deleteAll();
        addressRepository.deleteAll();
        userRepository.deleteAll();

        // Rôle OWNER (récupération ou création)
        Role ownerRole = roleRepository.findByName(RoleName.OWNER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.OWNER);
                    return roleRepository.save(role);
                });

        // Création OWNER
        ownerUser = new User();
        ownerUser.setEmail("owner@test.com");
        ownerUser.setFirstName("Propriétaire");
        ownerUser.setLastName("Test");
        ownerUser.setPassword(passwordEncoder.encode("password123"));
        ownerUser.setUserStatus(UserStatus.ACTIVE);
        ownerUser.setRoles(Set.of(ownerRole));
        ownerUser = userRepository.save(ownerUser);

        ownerToken = jwtService.generateAccessToken(ownerUser);

        // Adresse
        Address address = new Address();
        address.setStreet("Avenue des Champs");
        address.setNumber("50");
        address.setCity("Lyon");
        address.setPostalCode("69001");
        address.setCountry("France");
        address = addressRepository.save(address);

        // Location
        ChargingLocation location = new ChargingLocation();
        location.setLatitude(new BigDecimal("45.7640"));
        location.setLongitude(new BigDecimal("4.8357"));
        location.setAvailable(true);
        location.setAddress(address);
        location = locationRepository.save(location);

        // Station (available=true => doit compter comme availableStations)
        ownerStation = new ChargingStation();
        ownerStation.setName("Borne Propriétaire Lyon");
        ownerStation.setHourlyPrice(new BigDecimal("10.00"));
        ownerStation.setChargingPowerKw(new BigDecimal("50.00"));
        ownerStation.setChargingPower("50 kW");
        ownerStation.setAvailable(true);
        ownerStation.setHasStand(true);
        ownerStation.setChargingLocation(location);
        ownerStation.setUser(ownerUser);
        ownerStation = stationRepository.save(ownerStation);
    }

    @Test
    @DisplayName("INTÉGRATION : OWNER accède à /api/owner/dashboard/stats et récupère les métriques")
    void ownerCanAccessDashboardStats() throws Exception {
        // GIVEN
        User client = createClientUser();

        // Booking 1 : CONFIRMED + paiement SUCCEEDED (30€)
        Booking booking1 = createBooking(client, ownerStation, BookingStatus.CONFIRMED, new BigDecimal("30.00"));
        createPayment(booking1, PaymentStatus.SUCCEEDED);

        // Booking 2 : CONFIRMED + paiement SUCCEEDED (50€)
        Booking booking2 = createBooking(client, ownerStation, BookingStatus.CONFIRMED, new BigDecimal("50.00"));
        createPayment(booking2, PaymentStatus.SUCCEEDED);

        // Booking 3 : PENDING (ne doit pas impacter confirmedBookings ; revenue dépend de ton service)
        createBooking(client, ownerStation, BookingStatus.PENDING, new BigDecimal("20.00"));

        // WHEN & THEN
        mockMvc.perform(get("/api/owner/dashboard/stats")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                // DTO réel : OwnerDashboardStatsDto
                .andExpect(jsonPath("$.totalRevenue").value(80.00))      // 30 + 50
                .andExpect(jsonPath("$.totalBookings").value(3))
                .andExpect(jsonPath("$.confirmedBookings").value(2))
                .andExpect(jsonPath("$.totalStations").value(1))
                .andExpect(jsonPath("$.availableStations").value(1));
    }

    @Test
    @DisplayName("INTÉGRATION : USER ne peut PAS accéder à /api/owner/dashboard/stats (403)")
    void regularUserCannotAccessDashboardStats() throws Exception {
        // GIVEN : user avec rôle USER
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.USER);
                    return roleRepository.save(role);
                });

        User regularUser = new User();
        regularUser.setEmail("simple-user@test.com");
        regularUser.setFirstName("Utilisateur");
        regularUser.setLastName("Simple");
        regularUser.setPassword(passwordEncoder.encode("password123"));
        regularUser.setUserStatus(UserStatus.ACTIVE);
        regularUser.setRoles(Set.of(userRole));
        regularUser = userRepository.save(regularUser);

        String userToken = jwtService.generateAccessToken(regularUser);

        // WHEN & THEN : RBAC attendu
        mockMvc.perform(get("/api/owner/dashboard/stats")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    // ===================== HELPERS =====================

    private User createClientUser() {
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.USER);
                    return roleRepository.save(role);
                });

        User client = new User();
        client.setEmail("client-dashboard@test.com");
        client.setFirstName("Client");
        client.setLastName("Test");
        client.setPassword(passwordEncoder.encode("password123"));
        client.setUserStatus(UserStatus.ACTIVE);
        client.setRoles(Set.of(userRole));
        return userRepository.save(client);
    }

    private Booking createBooking(User user, ChargingStation station, BookingStatus status, BigDecimal amount) {
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setChargingStation(station);
        booking.setStartDateTime(LocalDateTime.now().plusDays(1));
        booking.setEndDateTime(LocalDateTime.now().plusDays(1).plusHours(3));
        booking.setBookingStatus(status);
        booking.setTotalAmount(amount);
        return bookingRepository.save(booking);
    }

    private Payment createPayment(Booking booking, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setStripePaymentIntentId("pi_test_" + UUID.randomUUID());
        payment.setPaymentStatus(status);
        payment = paymentRepository.save(payment);

        booking.setPayment(payment);
        bookingRepository.save(booking);

        return payment;
    }
}
