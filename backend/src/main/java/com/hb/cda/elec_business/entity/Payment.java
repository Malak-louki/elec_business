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
    String stripePaymentIntentId;
    private String paymentStatus;

    @OneToOne(mappedBy = "payment")
    private Booking booking;

}
