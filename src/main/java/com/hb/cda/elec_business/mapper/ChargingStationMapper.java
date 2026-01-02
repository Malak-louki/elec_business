package com.hb.cda.elec_business.mapper;

import com.hb.cda.elec_business.dto.charging.*;
import com.hb.cda.elec_business.entity.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ChargingStationMapper {

    /**
     * Convertit une entité ChargingStation en DTO Response
     */
    public static ChargingStationResponseDto toResponseDto(ChargingStation station) {
        if (station == null) {
            log.warn("Tentative de conversion d'une station null");
            return null;
        }

        try {
            ChargingStationResponseDto.ChargingStationResponseDtoBuilder builder = ChargingStationResponseDto.builder()
                    .id(station.getId())
                    .name(station.getName())
                    .hourlyPrice(station.getHourlyPrice())
                    .chargingPowerKw(station.getChargingPowerKw())  // Numérique
                    .chargingPower(station.getChargingPower())      // Avec unité
                    .instruction(station.getInstruction())
                    .hasStand(station.getHasStand())
                    .mediaUrl(station.getMedia())
                    .available(station.isAvailable())
                    .createdAt(station.getCreatedAt())
                    .updatedAt(station.getUpdatedAt());

            // Gestion sécurisée du propriétaire
            if (station.getUser() != null) {
                builder.owner(toOwnerInfoDto(station.getUser()));
            } else {
                log.warn("Station {} n'a pas de propriétaire", station.getId());
            }

            // Gestion sécurisée de la location et de l'adresse
            if (station.getChargingLocation() != null) {
                builder.location(toLocationDto(station.getChargingLocation()));

                if (station.getChargingLocation().getAddress() != null) {
                    builder.address(toAddressDto(station.getChargingLocation().getAddress()));
                } else {
                    log.warn("Station {} n'a pas d'adresse", station.getId());
                }
            } else {
                log.warn("Station {} n'a pas de ChargingLocation", station.getId());
            }

            // Gestion sécurisée des disponibilités
            if (station.getAvailabilities() != null) {
                builder.availabilities(toAvailabilityDtoList(station.getAvailabilities()));
            } else {
                builder.availabilities(Collections.emptyList());
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Erreur lors de la conversion de la station {} en DTO: {}",
                    station.getId(), e.getMessage(), e);
            throw new RuntimeException("Erreur de mapping pour la station " + station.getId(), e);
        }
    }

    /**
     * Convertit les infos du propriétaire en DTO
     */
    public static ChargingStationResponseDto.OwnerInfoDto toOwnerInfoDto(User user) {
        if (user == null) {
            return null;
        }

        try {
            return ChargingStationResponseDto.OwnerInfoDto.builder()
                    .id(user.getId())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .build();
        } catch (Exception e) {
            log.error("Erreur lors de la conversion du user {} en OwnerInfoDto: {}",
                    user.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Convertit une Address en AddressDto
     */
    public static AddressDto toAddressDto(Address address) {
        if (address == null) {
            return null;
        }

        try {
            return AddressDto.builder()
                    .street(address.getStreet())
                    .number(address.getNumber())
                    .city(address.getCity())
                    .postalCode(address.getPostalCode())
                    .country(address.getCountry())
                    .build();
        } catch (Exception e) {
            log.error("Erreur lors de la conversion de l'adresse {} en DTO: {}",
                    address.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Convertit un AddressDto en entité Address
     */
    public static Address toAddressEntity(AddressDto dto) {
        if (dto == null) {
            return null;
        }

        Address address = new Address();
        address.setStreet(dto.getStreet());
        address.setNumber(dto.getNumber());
        address.setCity(dto.getCity());
        address.setPostalCode(dto.getPostalCode());
        address.setCountry(dto.getCountry());
        return address;
    }

    /**
     * Convertit une ChargingLocation en LocationDto
     */
    public static LocationDto toLocationDto(ChargingLocation location) {
        if (location == null) {
            return null;
        }

        try {
            return LocationDto.builder()
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .build();
        } catch (Exception e) {
            log.error("Erreur lors de la conversion de la location {} en DTO: {}",
                    location.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Convertit un LocationDto en entité ChargingLocation
     */
    public static ChargingLocation toLocationEntity(LocationDto dto, Address address) {
        if (dto == null) {
            return null;
        }

        ChargingLocation location = new ChargingLocation();
        location.setLatitude(dto.getLatitude());
        location.setLongitude(dto.getLongitude());
        location.setAvailable(true);
        location.setAddress(address);
        return location;
    }

    /**
     * Convertit une Availability en AvailabilityDto
     */
    public static AvailabilityDto toAvailabilityDto(Availability availability) {
        if (availability == null) {
            return null;
        }

        try {
            return AvailabilityDto.builder()
                    .id(availability.getId())
                    .day(availability.getDay())
                    .startTime(availability.getStartTime())
                    .endTime(availability.getEndTime())
                    .build();
        } catch (Exception e) {
            log.error("Erreur lors de la conversion de l'availability {} en DTO: {}",
                    availability.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Convertit un AvailabilityDto en entité Availability
     */
    public static Availability toAvailabilityEntity(AvailabilityDto dto) {
        if (dto == null) {
            return null;
        }

        Availability availability = new Availability();
        availability.setDay(dto.getDay());
        availability.setStartTime(dto.getStartTime());
        availability.setEndTime(dto.getEndTime());
        return availability;
    }

    /**
     * Convertit une liste d'Availability en liste de DTOs
     */
    public static List<AvailabilityDto> toAvailabilityDtoList(java.util.Set<Availability> availabilities) {
        if (availabilities == null || availabilities.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return availabilities.stream()
                    .map(ChargingStationMapper::toAvailabilityDto)
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors de la conversion de la liste d'availabilities: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Convertit une liste de ChargingStation en liste de DTOs
     */
    public static List<ChargingStationResponseDto> toResponseDtoList(List<ChargingStation> stations) {
        if (stations == null || stations.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return stations.stream()
                    .map(ChargingStationMapper::toResponseDto)
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erreur lors de la conversion de la liste de stations: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}