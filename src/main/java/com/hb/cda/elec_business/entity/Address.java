package com.hb.cda.elec_business.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Address {
    @Id
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
