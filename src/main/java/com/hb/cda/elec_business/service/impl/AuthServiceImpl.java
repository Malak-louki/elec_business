package com.hb.cda.elec_business.service.impl;

import com.hb.cda.elec_business.dto.AuthResponseDto;
import com.hb.cda.elec_business.dto.LoginRequestDto;
import com.hb.cda.elec_business.dto.RegisterRequestDto;
import com.hb.cda.elec_business.dto.UserResponseDto;
import com.hb.cda.elec_business.entity.Role;
import com.hb.cda.elec_business.entity.RoleName;
import com.hb.cda.elec_business.entity.User;
import com.hb.cda.elec_business.entity.UserStatus;
import com.hb.cda.elec_business.mapper.UserMapper;
import com.hb.cda.elec_business.repository.RoleRepository;
import com.hb.cda.elec_business.repository.UserRepository;
import com.hb.cda.elec_business.security.JwtService;
import com.hb.cda.elec_business.service.AuthService;
import com.hb.cda.elec_business.service.UserValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserValidationService validationService;

    @Override
    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        // 1. Règles métier
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Phone number already exists");
        }

        // 2. Rôle par défaut
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("Default role USER not found"));

        // 3. Construction de l'entité User
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUserStatus(UserStatus.PENDING);
        user.setFirstName(request.getUsername());
        user.setLastName(request.getLastName());
        user.setRoles(Collections.singleton(userRole));

        // 4. Persistance
        User savedUser = userRepository.save(user);

        //5. Création do code de validation et envoi de l'email
        validationService.createAndSendValidation(savedUser);

        // 6. Génération du token (même si le compte n'est pas encore actif)
        // L'utilisateur ne pourra pas l'utiliser tant qu'il n'a pas validé son email
        String accessToken = jwtService.generateAccessToken(savedUser);

        // 6. Mapping DTO
        UserResponseDto userDto = UserMapper.toDto(savedUser);

        return AuthResponseDto.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .user(userDto)
                .build();
    }

    @Override
    public AuthResponseDto login(LoginRequestDto loginRequestDto) {
        // 1. Authentification Spring Security (email + password)
        // Spring Security vérifiera automatiquement si le compte est enabled (ACTIVE)
        // grâce à la méthode isEnabled() de User
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequestDto.getEmail(),
                            loginRequestDto.getPassword()
                    )
            );

            // 2. Récupération de l'utilisateur authentifié
            User user = (User) authentication.getPrincipal();

            //3. Double Vérification du statut (sécurité)
            if (user.getUserStatus() != UserStatus.ACTIVE) {
                throw new DisabledException("Account is not activated yet, please check your email.");
            }

            // 4. Génération du token JWT
            String accessToken = jwtService.generateAccessToken(user);

            // 5. Mapping vers DTO
            UserResponseDto userDto = UserMapper.toDto(user);

            // 6. Réponse
            return AuthResponseDto.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .user(userDto)
                    .build();
        }catch (DisabledException e) {
            throw new DisabledException("Account is not activated yet, please check your email to activate your account.");
        }

    }
}
