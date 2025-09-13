package com.hb.cda.elec_business.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Time;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
public class Availability {
    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private Date day;
    private Time startTime;
    private Time endTime;

    @ManyToMany(mappedBy = "availabilities")
    private Set<ChargingStation> chargingStations;

}
