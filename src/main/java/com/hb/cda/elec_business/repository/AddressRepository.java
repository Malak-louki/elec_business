package com.hb.cda.elec_business.repository;

import com.hb.cda.elec_business.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, String> {

    /**
     * Trouve une adresse exacte pour éviter les doublons
     */
    Optional<Address> findByStreetAndNumberAndCityAndPostalCodeAndCountry(
            String street,
            String number,
            String city,
            String postalCode,
            String country
    );
}
