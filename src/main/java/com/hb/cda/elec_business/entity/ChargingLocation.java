package com.hb.cda.elec_business.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class ChargingLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private String id;

    private Boolean isAvailable;

    @Column(precision = 10, scale = 8)
    @NotNull
    private BigDecimal latitude;
    @NotNull
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @ManyToOne
    @JoinColumn(name = "address_id")

    private Address address;

    @OneToMany(mappedBy = "chargingLocation")
    private Set<ChargingStation> chargingStations;


}
