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
public class Payment {
    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    String stripe_payment_entent_id;
    private String payment_status;

    @OneToMany(mappedBy = "payment")
    private Set<Booking> bookings;

}
