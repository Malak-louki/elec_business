package com.hb.cda.elec_business.service;

import com.hb.cda.elec_business.entity.Booking;
import com.hb.cda.elec_business.entity.ChargingStation;
import com.hb.cda.elec_business.repository.BookingRepository;
import com.hb.cda.elec_business.repository.ChargingStationRepository;
import com.hb.cda.elec_business.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
@RequiredArgsConstructor
@Service
public class BookingService {

    private final ChargingStationRepository stationRepo;
    private final BookingRepository bookingRepo;
    private  final UserRepository userRepo;
    @Transactional
    public Booking createBooking(String stationId,
                                 LocalDate startDate, LocalTime startHour,
                                 LocalDate endDate,   LocalTime endHour) {

        // 1) Verrouille la borne (serialize la création de réservations sur cette station)
        ChargingStation station = stationRepo.lockById(stationId)
                .orElseThrow(() -> new EntityNotFoundException("Station introuvable: " + stationId));

        // 2) Vérifie le chevauchement
        boolean overlaps = bookingRepo.existsOverlap(stationId, startDate, startHour, endDate, endHour);
        if (overlaps) {
            throw new IllegalStateException("Créneau indisponible (chevauchement).");
        }


        // 3) Crée et persiste la réservation
        Booking b = new Booking();
        b.setChargingStation(station);
        b.setStartDate(startDate);
        b.setStartHour(startHour);
        b.setEndDate(endDate);
        b.setEndHour(endHour);
        b.setUser();

        return bookingRepo.save(b);
    }
}

