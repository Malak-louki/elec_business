package com.hb.cda.elec_business.repository;

import com.hb.cda.elec_business.entity.Availability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, String> {

    /**
     * Trouve une disponibilité exacte pour éviter les doublons
     */
    Optional<Availability> findByDayAndStartTimeAndEndTime(
            LocalDate day,
            LocalTime startTime,
            LocalTime endTime
    );
}