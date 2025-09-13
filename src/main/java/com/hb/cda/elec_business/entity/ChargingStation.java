package com.hb.cda.elec_business.entity;

import jakarta.persistence.*;
import lombok.*;


import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class ChargingStation {
    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String name;
    private Double hourlyPrice;
    private String chargingPower;
    private String instruction;
    private Boolean onStand;
    private String media;
    private Boolean isAvailable;

    @ManyToOne
    private User user;

    @ManyToOne
    @JoinColumn(name = "charging_location_id")
    private ChargingLocation chargingLocation;

    @ManyToMany
    @JoinTable(
            name = "Charging_station_availibility",
            joinColumns = @JoinColumn(name = "Id_Charging_station"),
            inverseJoinColumns = @JoinColumn(name = "Id_Availibility")
    )
    private Set<Availability> availabilities;




}
