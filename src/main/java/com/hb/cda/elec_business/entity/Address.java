package com.hb.cda.elec_business.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class Address {
    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String street;
    private String number;
    private String city;
    private String postalCode;
    private String country;

    @OneToMany(mappedBy = "address")
    private Set<User> users;
    @OneToMany(mappedBy = "address")
    private Set<ChargingLocation> chargingLocations;
}
