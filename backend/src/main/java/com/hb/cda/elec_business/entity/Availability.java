package com.hb.cda.elec_business.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

import static jakarta.persistence.FetchType.LAZY;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class Availability {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotNull private LocalDate day;
    @NotNull private LocalTime startTime;
    @NotNull private LocalTime endTime;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "charging_station_id", nullable = false)
    private ChargingStation chargingStation;
}
