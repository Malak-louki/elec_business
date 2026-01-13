package com.hb.cda.elec_business.repository;

import com.hb.cda.elec_business.entity.Payment;
import com.hb.cda.elec_business.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    /**
     * Trouve un paiement par son Payment Intent ID (Stripe)
     */
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    /**
     * Trouve tous les paiements d'un statut donné
     */
    List<Payment> findByPaymentStatus(PaymentStatus status);
}