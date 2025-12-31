package com.hb.cda.elec_business.repository;

import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.entity.UserValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserValidationRepository extends JpaRepository<UserValidation, String> {

    /**
     * Trouve une validation par son code de confirmation
     */
    Optional<UserValidation> findByConfirmationCode(String confirmationCode);

    /**
     * Supprime toutes les validations non validées pour un utilisateur
     * Utile pour le renvoi d'email
     */
    void deleteByUserAndValidatedAtIsNull(User user);

    /**
     * Vérifie si un utilisateur a une validation en attente
     */
    boolean existsByUserAndValidatedAtIsNull(User user);

}
