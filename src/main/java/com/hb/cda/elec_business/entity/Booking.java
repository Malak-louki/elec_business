package com.hb.cda.elec_business.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;


import java.time.LocalDate;
import java.time.LocalTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(indexes = {
        @Index(name="idx_booking_station_start", columnList="Id_Charging_station, startDate, startHour"),
        @Index(name="idx_booking_station_end",   columnList="Id_Charging_station, endDate, endHour")
})
@Entity
public class Booking
{
    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private LocalDate startDate;
    private LocalTime startHour;
    private LocalTime endHour;
    private LocalDate endDate;
    @Positive
    private Double paidAmount;
    private String bookingStatus;
    private String invoicePath;
    @ManyToOne
    @JoinColumn(name = "Id_User")
    private User user;

    @OneToOne
    @JoinColumn(name = "Id_Payment", unique = true)
    private Payment payment;

    @ManyToOne
    @JoinColumn(name = "Id_Charging_station")
    private ChargingStation chargingStation;
}
