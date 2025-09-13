package com.hb.cda.elec_business.repository;

import com.hb.cda.elec_business.entity.ChargingStation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChargingStationRepository  extends JpaRepository<ChargingStation, String> {
    // Verrou pessimiste sur la ligne de la station
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ChargingStation s where s.id = :id")
    Optional<ChargingStation> lockById(@Param("id") String id);
}

