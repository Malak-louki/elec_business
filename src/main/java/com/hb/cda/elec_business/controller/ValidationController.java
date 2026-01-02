package com.hb.cda.elec_business.controller;

import com.hb.cda.elec_business.service.impl.UserValidationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ValidationController {
    private final UserValidationServiceImpl validationService;

    /**
     * Endpoint pour valider un compte utilisateur
     * GET /api/auth/validate?code=xxxxx
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, String>> validateAccount(
            @RequestParam("code") String confirmationCode
    ){
        boolean isValid = validationService.validateUserAccount(confirmationCode);

        if (isValid) {
            return ResponseEntity.ok(Map.of(
                    "message", "Votre compte a été activé avec succès !  vous pouvez maintenant vous connecter",
                    "status", "success"
            ));
        }else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Code de validation invalide ou expiré.",
                    "status", "error"
            ));
        }
    }

    /**
     * Endpoint pour renvoyer un email de validation
     * POST /api/auth/resend-validation
     */
    @PostMapping("/resend-validation")
    public ResponseEntity<Map<String, String>> resendValidationEmail(
            @RequestBody Map<String, String> request
    ){
        String email = request.get("email");

        if(email == null || email.isBlank()){
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Email requis",
                    "status", "error"
            ));
        }

        boolean sent = validationService.resendValidationEmail(email);

        if(sent){
            return ResponseEntity.ok(Map.of(
                    "message", "Un nouvel email de validation a été envoyé à " + email,
                    "status", "success"
            ));
        }else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Impossible de renvoyer l'email. Le compte est peut-être déjà actif ou l'email n'existe pas.",
                    "status", "error"
            ));
        }

    }
}
