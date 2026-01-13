package com.hb.cda.elec_business.service.impl;

import com.hb.cda.elec_business.dto.payment.PaymentRequestDto;
import com.hb.cda.elec_business.dto.payment.PaymentResponseDto;
import com.hb.cda.elec_business.entity.Booking;
import com.hb.cda.elec_business.entity.BookingStatus;
import com.hb.cda.elec_business.entity.Payment;
import com.hb.cda.elec_business.entity.PaymentStatus;
import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.repository.BookingRepository;
import com.hb.cda.elec_business.repository.PaymentRepository;
import com.hb.cda.elec_business.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public PaymentResponseDto simulatePayment(PaymentRequestDto request, User user) {
        log.info("Simulating payment for booking {} by user {}", request.getBookingId(), user.getEmail());

        // ═══════════════════════════════════════════════════════════════
        // ÉTAPE 1 : Vérifier que la réservation existe et est PENDING
        // ═══════════════════════════════════════════════════════════════

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Réservation non trouvée avec l'ID : " + request.getBookingId()
                ));

        // Vérifier que c'est bien la réservation de l'utilisateur
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "Cette réservation ne vous appartient pas"
            );
        }

        // Vérifier que la réservation est en attente de paiement
        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Cette réservation n'est pas en attente de paiement. Statut actuel : "
                            + booking.getBookingStatus()
            );
        }

        // ═══════════════════════════════════════════════════════════════
        // ÉTAPE 2 : Simuler le paiement
        // ═══════════════════════════════════════════════════════════════

        // Générer un "Payment Intent ID" simulé (comme Stripe)
        String simulatedPaymentIntentId = "pi_sim_" + UUID.randomUUID().toString().substring(0, 24);

        // Simuler succès ou échec selon le paramètre
        PaymentStatus paymentStatus = request.getSimulateSuccess()
                ? PaymentStatus.SUCCEEDED
                : PaymentStatus.FAILED;

        log.info("Payment simulation: {} - Status: {}", simulatedPaymentIntentId, paymentStatus);

        // ═══════════════════════════════════════════════════════════════
        // ÉTAPE 3 : Créer l'entité Payment
        // ═══════════════════════════════════════════════════════════════

        Payment payment = new Payment();
        payment.setStripePaymentIntentId(simulatedPaymentIntentId);
        payment.setPaymentStatus(paymentStatus);

        payment = paymentRepository.save(payment);
        log.info("Payment created with ID: {}", payment.getId());

        // ═══════════════════════════════════════════════════════════════
        // ÉTAPE 4 : Mettre à jour la réservation
        // ═══════════════════════════════════════════════════════════════

        if (paymentStatus == PaymentStatus.SUCCEEDED) {
            // Paiement réussi → Confirmer la réservation
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            booking.setPayment(payment);
            booking.setExpiresAt(null); // Plus de délai d'expiration

            log.info("Booking {} confirmed after successful payment", booking.getId());
        } else {
            // Paiement échoué → Garder en PENDING (ou passer en CANCELLED selon logique)
            log.warn("Payment failed for booking {}. Booking remains PENDING", booking.getId());
        }

        bookingRepository.save(booking);

        // ═══════════════════════════════════════════════════════════════
        // ÉTAPE 5 : Retourner le DTO de réponse
        // ═══════════════════════════════════════════════════════════════

        return PaymentResponseDto.builder()
                .id(payment.getId())
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .paymentStatus(payment.getPaymentStatus())
                .amount(booking.getTotalAmount())
                .currency("EUR")
                .paymentMethod(request.getPaymentMethod())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .bookingId(booking.getId())
                .bookingStatus(booking.getBookingStatus().name())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentById(String paymentId) {
        log.info("Fetching payment details for ID: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Paiement non trouvé avec l'ID : " + paymentId
                ));

        // Récupérer la réservation associée
        Booking booking = bookingRepository.findByPayment(payment)
                .orElse(null);

        return PaymentResponseDto.builder()
                .id(payment.getId())
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .paymentStatus(payment.getPaymentStatus())
                .amount(booking != null ? booking.getTotalAmount() : null)
                .currency("EUR")
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .bookingId(booking != null ? booking.getId() : null)
                .bookingStatus(booking != null ? booking.getBookingStatus().name() : null)
                .build();
    }

    @Override
    @Transactional
    public PaymentResponseDto refundPayment(String paymentId, User user) {
        log.info("Refunding payment {} for user {}", paymentId, user.getEmail());

        // ═══════════════════════════════════════════════════════════════
        // ÉTAPE 1 : Vérifier que le paiement existe
        // ═══════════════════════════════════════════════════════════════

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Paiement non trouvé avec l'ID : " + paymentId
                ));

        // Vérifier que le paiement est SUCCEEDED
        if (payment.getPaymentStatus() != PaymentStatus.SUCCEEDED) {
            throw new IllegalArgumentException(
                    "Impossible de rembourser un paiement qui n'a pas réussi. Statut actuel : "
                            + payment.getPaymentStatus()
            );
        }

        // ═══════════════════════════════════════════════════════════════
        // ÉTAPE 2 : Vérifier que c'est la réservation de l'utilisateur
        // ═══════════════════════════════════════════════════════════════

        Booking booking = bookingRepository.findByPayment(payment)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucune réservation trouvée pour ce paiement"
                ));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "Ce paiement ne vous appartient pas"
            );
        }

        // ═══════════════════════════════════════════════════════════════
        // ÉTAPE 3 : Simuler le remboursement
        // ═══════════════════════════════════════════════════════════════

        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        // Annuler la réservation
        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        log.info("Payment {} refunded and booking {} cancelled", paymentId, booking.getId());

        // ═══════════════════════════════════════════════════════════════
        // ÉTAPE 4 : Retourner le DTO
        // ═══════════════════════════════════════════════════════════════

        return PaymentResponseDto.builder()
                .id(payment.getId())
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .paymentStatus(payment.getPaymentStatus())
                .amount(booking.getTotalAmount())
                .currency("EUR")
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .bookingId(booking.getId())
                .bookingStatus(booking.getBookingStatus().name())
                .build();
    }
}