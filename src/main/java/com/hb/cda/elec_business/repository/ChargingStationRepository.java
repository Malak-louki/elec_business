package com.hb.cda.elec_business.repository;

import com.hb.cda.elec_business.entity.ChargingStation;
import com.hb.cda.elec_business.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChargingStationRepository extends JpaRepository<ChargingStation, String> {

    /**
     * Trouve une station par ID avec toutes ses relations chargées (évite LazyInitializationException)
     */
    @Query("SELECT DISTINCT cs FROM ChargingStation cs " +
            "LEFT JOIN FETCH cs.chargingLocation cl " +
            "LEFT JOIN FETCH cl.address " +
            "LEFT JOIN FETCH cs.availabilities " +
            "LEFT JOIN FETCH cs.user " +
            "WHERE cs.id = :id")
    Optional<ChargingStation> findByIdWithRelations(@Param("id") String id);

    /**
     * Trouve toutes les bornes d'un propriétaire avec relations
     */
    @Query("SELECT DISTINCT cs FROM ChargingStation cs " +
            "LEFT JOIN FETCH cs.chargingLocation cl " +
            "LEFT JOIN FETCH cl.address " +
            "LEFT JOIN FETCH cs.availabilities " +
            "LEFT JOIN FETCH cs.user " +
            "WHERE cs.user = :user")
    List<ChargingStation> findByUserWithRelations(@Param("user") User user);

    /**
     * Trouve toutes les bornes d'un propriétaire (version simple)
     */
    List<ChargingStation> findByUser(User user);

    /**
     * Trouve toutes les bornes disponibles avec relations
     */
    @Query("SELECT DISTINCT cs FROM ChargingStation cs " +
            "LEFT JOIN FETCH cs.chargingLocation cl " +
            "LEFT JOIN FETCH cl.address " +
            "LEFT JOIN FETCH cs.user " +
            "WHERE cs.available = true")
    List<ChargingStation> findByAvailableTrueWithRelations();

    /**
     * Trouve toutes les bornes disponibles
     */
    List<ChargingStation> findByAvailableTrue();

    /**
     * Recherche par prix maximum
     */
    List<ChargingStation> findByHourlyPriceLessThanEqualAndAvailableTrue(BigDecimal maxPrice);

    /**
     * Recherche par ville
     */
    @Query("SELECT cs FROM ChargingStation cs " +
            "JOIN cs.chargingLocation cl " +
            "JOIN cl.address a " +
            "WHERE LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%')) " +
            "AND cs.available = true")
    List<ChargingStation> findByCity(@Param("city") String city);

    /**
     * Recherche avancée avec filtres multiples
     * CORRIGÉ : utilise chargingPowerKw (numérique) au lieu de chargingPower (string)
     */
    @Query("SELECT cs FROM ChargingStation cs " +
            "JOIN cs.chargingLocation cl " +
            "JOIN cl.address a " +
            "WHERE cs.available = true " +
            "AND (:city IS NULL OR LOWER(a.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
            "AND (:maxPrice IS NULL OR cs.hourlyPrice <= :maxPrice) " +
            "AND (:minPowerKw IS NULL OR cs.chargingPowerKw >= :minPowerKw)")
    List<ChargingStation> searchStations(
            @Param("city") String city,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minPowerKw") BigDecimal minPowerKw
    );
}