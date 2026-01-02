package com.hb.cda.elec_business.service.impl;

import com.hb.cda.elec_business.dto.charging.AddressDto;
import com.hb.cda.elec_business.dto.charging.AvailabilityDto;
import com.hb.cda.elec_business.dto.charging.ChargingStationRequestDto;
import com.hb.cda.elec_business.dto.charging.ChargingStationResponseDto;
import com.hb.cda.elec_business.entity.*;
import com.hb.cda.elec_business.mapper.ChargingStationMapper;
import com.hb.cda.elec_business.repository.*;
import com.hb.cda.elec_business.service.ChargingStationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChargingStationServiceImpl implements ChargingStationService {

    private final ChargingStationRepository stationRepository;
    private final AddressRepository addressRepository;
    private final ChargingLocationRepository locationRepository;
    private final AvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public ChargingStationResponseDto createStation(ChargingStationRequestDto request, User owner) {
        log.info("Creating charging station '{}' for user {}", request.getName(), owner.getEmail());

        // 1. Auto-attribution du rôle OWNER si nécessaire
        ensureUserHasOwnerRole(owner);

        // 2. Gestion de l'adresse (réutilisation ou création)
        Address address = findOrCreateAddress(request.getAddress());

        // 3. Création de la ChargingLocation
        ChargingLocation location = ChargingStationMapper.toLocationEntity(request.getLocation(), address);
        location = locationRepository.save(location);

        // 4. Création de la ChargingStation
        ChargingStation station = new ChargingStation();
        station.setName(request.getName());
        station.setHourlyPrice(request.getHourlyPrice());

        // Gestion de la puissance (numérique + affichage)
        station.setChargingPowerKw(request.getChargingPowerKw());
        station.setChargingPower(formatPower(request.getChargingPowerKw()));

        station.setInstruction(request.getInstruction());
        station.setHasStand(request.getHasStand() != null ? request.getHasStand() : false);
        station.setMedia(request.getMediaUrl());
        station.setAvailable(true);
        station.setUser(owner);
        station.setChargingLocation(location);

        // 5. Gestion des disponibilités
        if (request.getAvailabilities() != null && !request.getAvailabilities().isEmpty()) {
            Set<Availability> availabilities = processAvailabilities(request.getAvailabilities());
            station.setAvailabilities(availabilities);
        }

        station = stationRepository.save(station);
        log.info("Charging station created successfully with ID: {}", station.getId());

        return ChargingStationMapper.toResponseDto(station);
    }

    @Override
    @Transactional
    public ChargingStationResponseDto updateStation(String stationId, ChargingStationRequestDto request, User owner) {
        log.info("Updating charging station {} by user {}", stationId, owner.getEmail());

        ChargingStation station = stationRepository.findByIdWithRelations(stationId)
                .orElseThrow(() -> new IllegalArgumentException("Charging station not found with ID: " + stationId));

        // Vérification que l'utilisateur est le propriétaire
        if (!station.getUser().getId().equals(owner.getId())) {
            throw new AccessDeniedException("You are not authorized to update this charging station");
        }

        // Mise à jour des champs simples
        station.setName(request.getName());
        station.setHourlyPrice(request.getHourlyPrice());

        // Mise à jour de la puissance (numérique + affichage)
        station.setChargingPowerKw(request.getChargingPowerKw());
        station.setChargingPower(formatPower(request.getChargingPowerKw()));

        station.setInstruction(request.getInstruction());
        station.setHasStand(request.getHasStand() != null ? request.getHasStand() : false);
        station.setMedia(request.getMediaUrl());

        // Mise à jour de l'adresse et de la location si nécessaire
        if (request.getAddress() != null && request.getLocation() != null) {
            Address address = findOrCreateAddress(request.getAddress());
            ChargingLocation location = station.getChargingLocation();
            location.setLatitude(request.getLocation().getLatitude());
            location.setLongitude(request.getLocation().getLongitude());
            location.setAddress(address);
            locationRepository.save(location);
        }

        // Mise à jour des disponibilités
        if (request.getAvailabilities() != null) {
            station.getAvailabilities().clear();
            Set<Availability> availabilities = processAvailabilities(request.getAvailabilities());
            station.setAvailabilities(availabilities);
        }

        station = stationRepository.save(station);
        log.info("Charging station {} updated successfully", stationId);

        return ChargingStationMapper.toResponseDto(station);
    }

    @Override
    @Transactional
    public void deleteStation(String stationId, User owner) {
        log.info("Deleting charging station {} by user {}", stationId, owner.getEmail());

        ChargingStation station = stationRepository.findByIdWithRelations(stationId)
                .orElseThrow(() -> new IllegalArgumentException("Charging station not found with ID: " + stationId));

        // Vérification que l'utilisateur est le propriétaire
        if (!station.getUser().getId().equals(owner.getId())) {
            throw new AccessDeniedException("You are not authorized to delete this charging station");
        }

        stationRepository.delete(station);
        log.info("Charging station {} deleted successfully", stationId);
    }

    @Override
    @Transactional(readOnly = true)
    public ChargingStationResponseDto getStationById(String stationId) {
        ChargingStation station = stationRepository.findByIdWithRelations(stationId)
                .orElseThrow(() -> new IllegalArgumentException("Charging station not found with ID: " + stationId));

        return ChargingStationMapper.toResponseDto(station);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStationResponseDto> getMyStations(User owner) {
        log.info("Fetching stations for owner: {}", owner.getEmail());
        List<ChargingStation> stations = stationRepository.findByUserWithRelations(owner);
        return ChargingStationMapper.toResponseDtoList(stations);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStationResponseDto> getAllAvailableStations() {
        log.info("Fetching all available charging stations");
        List<ChargingStation> stations = stationRepository.findByAvailableTrueWithRelations();
        return ChargingStationMapper.toResponseDtoList(stations);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStationResponseDto> searchStations(String city, BigDecimal maxPrice, BigDecimal minPowerKw) {
        log.info("Searching stations with filters - city: {}, maxPrice: {}, minPowerKw: {}", city, maxPrice, minPowerKw);
        List<ChargingStation> stations = stationRepository.searchStations(city, maxPrice, minPowerKw);
        return ChargingStationMapper.toResponseDtoList(stations);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChargingStationResponseDto> getStationsByCity(String city) {
        log.info("Fetching stations in city: {}", city);
        List<ChargingStation> stations = stationRepository.findByCity(city);
        return ChargingStationMapper.toResponseDtoList(stations);
    }

    // ====================================================================
    // MÉTHODES PRIVÉES UTILITAIRES
    // ====================================================================

    /**
     * Assure que l'utilisateur a le rôle OWNER
     * Si ce n'est pas le cas, on l'ajoute automatiquement
     */
    private void ensureUserHasOwnerRole(User user) {
        Role ownerRole = roleRepository.findByName(RoleName.OWNER)
                .orElseThrow(() -> new IllegalStateException("Role OWNER not found in database"));

        boolean hasOwnerRole = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.OWNER);

        if (!hasOwnerRole) {
            log.info("Adding OWNER role to user {}", user.getEmail());
            user.getRoles().add(ownerRole);
            userRepository.save(user);
        }
    }

    /**
     * Trouve une adresse existante ou en crée une nouvelle
     * Évite les doublons en BDD
     */
    private Address findOrCreateAddress(AddressDto dto) {
        return addressRepository
                .findByStreetAndNumberAndCityAndPostalCodeAndCountry(
                        dto.getStreet(),
                        dto.getNumber(),
                        dto.getCity(),
                        dto.getPostalCode(),
                        dto.getCountry()
                )
                .orElseGet(() -> {
                    log.info("Creating new address: {}, {}", dto.getStreet(), dto.getCity());
                    Address newAddress = ChargingStationMapper.toAddressEntity(dto);
                    return addressRepository.save(newAddress);
                });
    }

    /**
     * Traite les disponibilités : réutilise les existantes ou crée de nouvelles
     */
    private Set<Availability> processAvailabilities(List<AvailabilityDto> dtos) {
        Set<Availability> availabilities = new HashSet<>();

        for (AvailabilityDto dto : dtos) {
            Availability availability = availabilityRepository
                    .findByDayAndStartTimeAndEndTime(dto.getDay(), dto.getStartTime(), dto.getEndTime())
                    .orElseGet(() -> {
                        log.debug("Creating new availability: {} {} - {}",
                                dto.getDay(), dto.getStartTime(), dto.getEndTime());
                        Availability newAvailability = ChargingStationMapper.toAvailabilityEntity(dto);
                        return availabilityRepository.save(newAvailability);
                    });
            availabilities.add(availability);
        }

        return availabilities;
    }

    /**
     * Formate une puissance numérique en chaîne avec unité
     * Ex: 7.4 → "7.4 kW", 22 → "22 kW", 150 → "150 kW"
     */
    private String formatPower(BigDecimal powerKw) {
        if (powerKw == null) {
            return null;
        }

        // Supprime les zéros inutiles (22.00 → 22, 7.40 → 7.4)
        String formatted = powerKw.stripTrailingZeros().toPlainString();
        return formatted + " kW";
    }
}