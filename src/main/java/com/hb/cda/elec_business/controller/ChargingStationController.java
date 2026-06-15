package com.hb.cda.elec_business.controller;

import com.hb.cda.elec_business.dto.charging.ChargingStationRequestDto;
import com.hb.cda.elec_business.dto.charging.ChargingStationResponseDto;
import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.service.ChargingStationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/charging-stations")
@RequiredArgsConstructor
@Slf4j
public class ChargingStationController {

    private final ChargingStationService stationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChargingStationResponseDto> createStation(
            @Valid @RequestBody ChargingStationRequestDto request,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("Creating station '{}' for user {}", request.getName(), currentUser.getEmail());

        ChargingStationResponseDto response = stationService.createStation(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChargingStationResponseDto> updateStation(
            @PathVariable String id,
            @Valid @RequestBody ChargingStationRequestDto request,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("Updating station {} by user {}", id, currentUser.getEmail());

        ChargingStationResponseDto response = stationService.updateStation(id, request, currentUser);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteStation(
            @PathVariable String id,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("Deleting station {} by user {}", id, currentUser.getEmail());

        stationService.deleteStation(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChargingStationResponseDto> getStationById(@PathVariable String id) {
        log.info("Getting station details for ID: {}", id);
        ChargingStationResponseDto response = stationService.getStationById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChargingStationResponseDto>> getMyStations(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("Getting stations for owner: {}", currentUser.getEmail());

        List<ChargingStationResponseDto> stations = stationService.getMyStations(currentUser);
        return ResponseEntity.ok(stations);
    }

    @GetMapping
    public ResponseEntity<List<ChargingStationResponseDto>> getAllAvailableStations() {
        log.info("Getting all available charging stations");
        List<ChargingStationResponseDto> stations = stationService.getAllAvailableStations();
        return ResponseEntity.ok(stations);
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<List<ChargingStationResponseDto>> getStationsByCity(@PathVariable String city) {
        log.info("Searching stations in city: {}", city);
        List<ChargingStationResponseDto> stations = stationService.getStationsByCity(city);
        return ResponseEntity.ok(stations);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ChargingStationResponseDto>> searchStations(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minPowerKw
    ) {
        log.info("Searching stations - city: {}, maxPrice: {}, minPowerKw: {}", city, maxPrice, minPowerKw);
        List<ChargingStationResponseDto> stations = stationService.searchStations(city, maxPrice, minPowerKw);
        return ResponseEntity.ok(stations);
    }
}