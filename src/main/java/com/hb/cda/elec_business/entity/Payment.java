package com.hb.cda.elec_business.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    String stripe_payment_entent_id;
    private String payment_status;

    @OneToMany(mappedBy = "payment")
    private Set<Booking> bookings;

}
