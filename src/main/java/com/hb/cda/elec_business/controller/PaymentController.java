package com.hb.cda.elec_business.controller;

import com.hb.cda.elec_business.dto.payment.PaymentRequestDto;
import com.hb.cda.elec_business.dto.payment.PaymentResponseDto;
import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller pour gérer les paiements
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Simule un paiement pour une réservation
     * POST /api/payments/simulate
     */
    @PostMapping("/simulate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponseDto> simulatePayment(
            @Valid @RequestBody PaymentRequestDto request,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("User {} attempting to pay for booking {}",
                currentUser.getEmail(), request.getBookingId());

        PaymentResponseDto response = paymentService.simulatePayment(request, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupère les détails d'un paiement
     * GET /api/payments/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponseDto> getPaymentById(@PathVariable String id) {
        log.info("Fetching payment details for ID: {}", id);

        PaymentResponseDto response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Rembourse un paiement (annule la réservation)
     * POST /api/payments/{id}/refund
     */
    @PostMapping("/{id}/refund")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentResponseDto> refundPayment(
            @PathVariable String id,
            Authentication authentication
    ) {
        User currentUser = (User) authentication.getPrincipal();
        log.info("User {} requesting refund for payment {}", currentUser.getEmail(), id);

        PaymentResponseDto response = paymentService.refundPayment(id, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de test pour vérifier que le module fonctionne
     * GET /api/payments/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("module", "Payment");
        response.put("message", "Payment module is running");
        return ResponseEntity.ok(response);
    }
}