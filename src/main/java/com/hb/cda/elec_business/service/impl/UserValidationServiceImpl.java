package com.hb.cda.elec_business.service.impl;

import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.entity.UserStatus;
import com.hb.cda.elec_business.entity.UserValidation;
import com.hb.cda.elec_business.repository.UserRepository;
import com.hb.cda.elec_business.repository.UserValidationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidationServiceImpl {

    private final UserValidationRepository validationRepository;
    private final UserRepository userRepository;
    private final EmailServiceImpl emailService;

    @Value("${app.validation.token-expiration}")
    private long tokenExpirationMs;

    /**
     * Crée un code de validation et envoie l'email
     * @param user L'utilisateur à valider
     */
    @Transactional
    public void createAndSendValidation(User user) {
        // Génère un code unique
        String confirmationCode = UUID.randomUUID().toString();

        // Crée l'entité UserValidation
        UserValidation validation = new UserValidation();
        validation.setConfirmationCode(confirmationCode);
        validation.setUser(user);
        // validatedAt reste null jusqu'à la validation

        validationRepository.save(validation);

        log.info("Code de validation créé pour l'utilisateur {}", user.getEmail());

        // Envoie l'email (asynchrone)
        emailService.sendValidationEmail(
                user.getEmail(),
                user.getFirstName(),
                confirmationCode
        );
    }

    /**
     * Valide un compte utilisateur avec le code de confirmation
     * @param confirmationCode Le code reçu par email
     * @return true si la validation a réussi, false sinon
     */
    @Transactional
    public boolean validateUserAccount(String confirmationCode) {
        Optional<UserValidation> validationOpt = validationRepository
                .findByConfirmationCode(confirmationCode);

        if (validationOpt.isEmpty()) {
            log.warn("Code de validation invalide ou inexistant: {}", confirmationCode);
            return false;
        }

        UserValidation validation = validationOpt.get();

        // Vérifie si déjà validé
        if (validation.getValidatedAt() != null) {
            log.warn("Code de validation déjà utilisé: {}", confirmationCode);
            return false;
        }

        // Vérifie l'expiration (24h par défaut)
        Instant createdAt = validation.getCreatedAt();
        Instant expirationTime = createdAt.plusSeconds(tokenExpirationMs / 1000);

        if (Instant.now().isAfter(expirationTime)) {
            log.warn("Code de validation expiré pour l'utilisateur {}",
                    validation.getUser().getEmail());
            return false;
        }

        // Active le compte
        User user = validation.getUser();
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        // Marque la validation comme effectuée
        validation.setValidatedAt(Instant.now());
        validationRepository.save(validation);

        log.info("Compte utilisateur {} validé avec succès", user.getEmail());

        // Envoie un email de confirmation
        emailService.sendAccountActivatedEmail(user.getEmail(), user.getFirstName());

        return true;
    }

    /**
     * Renvoie un email de validation si le précédent a expiré
     * @param email Email de l'utilisateur
     * @return true si l'email a été renvoyé, false sinon
     */
    @Transactional
    public boolean resendValidationEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            log.warn("Tentative de renvoi d'email pour un utilisateur inexistant: {}", email);
            return false;
        }

        User user = userOpt.get();

        // Vérifie que le compte n'est pas déjà actif
        if (user.getUserStatus() == UserStatus.ACTIVE) {
            log.info("Compte déjà actif pour {}, aucun email envoyé", email);
            return false;
        }

        // Invalide les anciennes validations non utilisées
        validationRepository.deleteByUserAndValidatedAtIsNull(user);

        // Crée une nouvelle validation
        createAndSendValidation(user);

        log.info("Email de validation renvoyé à {}", email);
        return true;
    }
}