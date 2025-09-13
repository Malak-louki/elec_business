package com.hb.cda.elec_business.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class ChargingStation {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank
    private String name;

    @Positive
    private Double hourlyPrice;


    private String chargingPower;

    private String instruction;
    private Boolean onStand;
    private String media;
    private Boolean isAvailable;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "charging_location_id")
    private ChargingLocation chargingLocation;

    // UNE station ➜ PLUSIEURS créneaux
    @OneToMany(mappedBy = "chargingStation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Availability> availabilities;
}
