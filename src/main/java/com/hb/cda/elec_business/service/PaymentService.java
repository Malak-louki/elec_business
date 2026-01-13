package com.hb.cda.elec_business.service;

import com.hb.cda.elec_business.dto.payment.PaymentRequestDto;
import com.hb.cda.elec_business.dto.payment.PaymentResponseDto;
import com.hb.cda.elec_business.entity.User;

    public interface PaymentService {

        /**
         * Simule un paiement pour une réservation
         *
         * @param request DTO contenant bookingId et méthode de paiement
         * @param user Utilisateur qui paie
         * @return Détails du paiement créé
         * @throws IllegalArgumentException si la réservation n'existe pas ou n'est pas PENDING
         */
        PaymentResponseDto simulatePayment(PaymentRequestDto request, User user);

        /**
         * Récupère les détails d'un paiement
         *
         * @param paymentId ID du paiement
         * @return Détails du paiement
         */
        PaymentResponseDto getPaymentById(String paymentId);

        /**
         * Annule un paiement (remboursement simulé)
         *
         * @param paymentId ID du paiement à annuler
         * @param user Utilisateur demandant le remboursement
         * @return Paiement mis à jour avec statut REFUNDED
         */
        PaymentResponseDto refundPayment(String paymentId, User user);
    }
