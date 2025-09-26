package com.hb.cda.elec_business.repository;

import com.hb.cda.elec_business.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;

public interface BookingRepository  extends JpaRepository<Booking, String> {
    @Query("""
       select (count(b) > 0)
       from Booking b
       where b.chargingStation.id = :stationId
         and b.bookingStatus <> 'CANCELED'
         and (
              (b.startDate < :endDate or (b.startDate = :endDate and b.startHour < :endHour))
          and (b.endDate   > :startDate or (b.endDate   = :startDate and b.endHour   > :startHour))
         )
    """)
    boolean existsOverlap(
            @Param("stationId") String stationId,
            @Param("startDate") LocalDate startDate,
            @Param("startHour") LocalTime startHour,
            @Param("endDate") LocalDate endDate,
            @Param("endHour") LocalTime endHour
    );
}
