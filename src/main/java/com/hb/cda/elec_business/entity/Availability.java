package com.hb.cda.elec_business.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Time;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Availability {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private Date day;
    private Time startTime;
    private Time endTime;

    @ManyToMany(mappedBy = "availibilities")
    private Set<ChargingStation> chargingStations;

}
