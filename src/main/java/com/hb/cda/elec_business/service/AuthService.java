package com.hb.cda.elec_business.service;

import com.hb.cda.elec_business.dto.auth.AuthResponseDto;
import com.hb.cda.elec_business.dto.auth.LoginRequestDto;
import com.hb.cda.elec_business.dto.auth.RegisterRequestDto;

public interface AuthService {
    /**
     * Inscription d’un nouvel utilisateur.
     * - Vérifie que l’email / téléphone ne sont pas déjà utilisés
     * - Crée un User avec rôle USER
     * - Hash le mot de passe
     * - Retourne le user + (plus tard) un JWT dans AuthResponseDto
     */
    AuthResponseDto register(RegisterRequestDto registerRequestDto);

    /**
     * Authentification par email + mot de passe.
     * - Vérifie credentials
     * - Retourne le user + token (quand on aura le JWT)
     */
    AuthResponseDto login(LoginRequestDto loginRequestDto);
}
