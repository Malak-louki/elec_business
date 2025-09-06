package com.hb.cda.elec_business.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.sql.Time;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Booking
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private Date startDate;
    private Date endDate;
    private Time startHour;
    private Time endHour;
    private Double paidAmount;
    private String bookingStatus;
    private String invoicePath;
    @ManyToOne
    @JoinColumn(name = "Id_User")
    private User user;

    @ManyToOne
    @JoinColumn(name = "Id_Payment")
    private Payment payment;

    @ManyToOne
    @JoinColumn(name = "Id_Charging_station")
    private ChargingStation chargingStation;
}
