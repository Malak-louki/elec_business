package com.hb.cda.elec_business.service;

import com.hb.cda.elec_business.dto.charging.ChargingStationRequestDto;
import com.hb.cda.elec_business.dto.charging.ChargingStationResponseDto;
import com.hb.cda.elec_business.entity.User;

import java.math.BigDecimal;
import java.util.List;

public interface ChargingStationService {

    /**
     * Créer une nouvelle charging station
     * Le rôle OWNER est automatiquement attribué à l'utilisateur si nécessaire
     */
    ChargingStationResponseDto createStation(ChargingStationRequestDto request, User owner);

    /**
     * Modifier une charging station existante
     * Seul le propriétaire peut modifier sa borne
     */
    ChargingStationResponseDto updateStation(String stationId, ChargingStationRequestDto request, User owner);

    /**
     * Supprimer une charging station
     * Seul le propriétaire peut supprimer sa borne
     */
    void deleteStation(String stationId, User owner);

    /**
     * Récupérer les détails d'une charging station par ID
     */
    ChargingStationResponseDto getStationById(String stationId);

    /**
     * Récupérer toutes les charging stations d'un propriétaire
     */
    List<ChargingStationResponseDto> getMyStations(User owner);

    /**
     * Récupérer toutes les charging stations disponibles
     */
    List<ChargingStationResponseDto> getAllAvailableStations();

    /**
     * Recherche avancée avec filtres
     * @param city Ville (optionnel)
     * @param maxPrice Prix maximum par heure (optionnel)
     * @param minPowerKw Puissance minimale en kW (optionnel) - CORRIGÉ : BigDecimal
     */
    List<ChargingStationResponseDto> searchStations(String city, BigDecimal maxPrice, BigDecimal minPowerKw);

    /**
     * Rechercher des charging stations par ville
     */
    List<ChargingStationResponseDto> getStationsByCity(String city);
}