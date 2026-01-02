package com.hb.cda.elec_business.repository;

import com.hb.cda.elec_business.entity.ChargingLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChargingLocationRepository extends JpaRepository<ChargingLocation, String> {

}