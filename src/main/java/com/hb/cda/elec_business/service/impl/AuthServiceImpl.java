package com.hb.cda.elec_business.service.impl;

import com.hb.cda.elec_business.dto.auth.AuthResponseDto;
import com.hb.cda.elec_business.dto.auth.LoginRequestDto;
import com.hb.cda.elec_business.dto.auth.RegisterRequestDto;
import com.hb.cda.elec_business.dto.auth.UserResponseDto;
import com.hb.cda.elec_business.entity.Role;
import com.hb.cda.elec_business.entity.RoleName;
import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.entity.UserStatus;
import com.hb.cda.elec_business.mapper.UserMapper;
import com.hb.cda.elec_business.repository.RoleRepository;
import com.hb.cda.elec_business.repository.UserRepository;
import com.hb.cda.elec_business.security.JwtService;
import com.hb.cda.elec_business.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserValidationServiceImpl validationService;

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        log.info("=== AuthService.register() CALLED ===");
        log.info("Checking if email exists: {}", request.getEmail());

        // 1. Règles métier
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email already exists: {}", request.getEmail());
            throw new IllegalArgumentException("Email already exists");
        }

        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            log.warn("Phone number already exists: {}", request.getPhone());
            throw new IllegalArgumentException("Phone number already exists");
        }

        // 2. Rôle par défaut - Le rôle USER existe maintenant grâce au RoleInitializer
        log.info("Fetching USER role...");
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("USER role not found - RoleInitializer may have failed"));
        log.info("Role found: {}", userRole.getName());

        // 3. Construction de l'entité User
        log.info("Creating new user entity...");
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserStatus(UserStatus.PENDING);
        user.setFirstName(request.getUsername());
        user.setLastName(request.getLastName());
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        // 4. Persistance
        log.info("Saving user to database...");
        User savedUser = userRepository.save(user);
        log.info("User saved with ID: {}", savedUser.getId());

        // 5. Création du code de validation et envoi de l'email
        log.info("Creating validation code and sending email...");
        try {
            validationService.createAndSendValidation(savedUser);
            log.info("Validation email sent successfully");
        } catch (Exception e) {
            log.error("Failed to send validation email", e);
            // Continue quand même, l'utilisateur est créé
        }

        // 6. Génération du token
        log.info("Generating JWT token...");
        String accessToken = jwtService.generateAccessToken(savedUser);

        // 7. Mapping DTO
        log.info("Mapping user to DTO...");
        UserResponseDto userDto = UserMapper.toDto(savedUser);

        log.info("=== Registration complete for: {} ===", request.getEmail());
        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .user(userDto)
                .build();
    }

    @Override
    public AuthResponseDto login(LoginRequestDto loginRequestDto) {
        log.info("=== AuthService.login() CALLED ===");
        log.info("Attempting authentication for: {}", loginRequestDto.getEmail());

        try {
            // 1. Authentification Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequestDto.getEmail(),
                            loginRequestDto.getPassword()
                    )
            );
            log.info("Authentication successful");

            // 2. Récupération de l'utilisateur authentifié
            User user = (User) authentication.getPrincipal();
            log.info("User retrieved: {} (ID: {})", user.getEmail(), user.getId());

            // 3. Double Vérification du statut
            if (user.getUserStatus() != UserStatus.ACTIVE) {
                log.warn("User status is not ACTIVE: {}", user.getUserStatus());
                throw new DisabledException("Account is not activated yet, please check your email.");
            }

            // 4. Génération du token JWT
            log.info("Generating JWT token...");
            String accessToken = jwtService.generateAccessToken(user);

            // 5. Mapping vers DTO
            UserResponseDto userDto = UserMapper.toDto(user);

            log.info("=== Login complete for: {} ===", loginRequestDto.getEmail());
            return AuthResponseDto.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .user(userDto)
                    .build();
        } catch (DisabledException e) {
            log.warn("Login failed - Account disabled: {}", loginRequestDto.getEmail());
            throw new DisabledException("Account is not activated yet, please check your email to activate your account.");
        } catch (Exception e) {
            log.error("Login failed for: {}", loginRequestDto.getEmail(), e);
            throw e;
        }
    }
}