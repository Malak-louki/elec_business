package com.hb.cda.elec_business.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class ChargingLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private Boolean isAvailable;
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @ManyToOne
    @JoinColumn(name = "Id_Adress")
    private Address address;

    @OneToMany(mappedBy = "chargingLocation")
    private Set<ChargingStation> chargingStations;
}
